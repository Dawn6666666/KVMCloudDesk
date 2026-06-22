package com.example.kvm.backend.service.libvirt;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.VmService;
import com.example.kvm.common.dto.VmInfoDto;
import com.example.kvm.common.request.CreateVmRequest;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

@Service
@Profile("libvirt")
public class LibvirtVmService implements VmService {
    private final LibvirtConnectionManager manager;
    private final Path imageDir;

    public LibvirtVmService(LibvirtConnectionManager manager,
                            @Value("${kvm.image.dir}") String imageDir) {
        this.manager = manager;
        this.imageDir = Path.of(imageDir);
    }

    @Override
    public List<VmInfoDto> listVms() {
        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        PointerByReference domainsRef = new PointerByReference();
        try {
            int count = lib.virConnectListAllDomains(conn, domainsRef, 0);
            check(count, "获取虚拟机列表失败");
            List<VmInfoDto> result = new ArrayList<>();
            Pointer domains = domainsRef.getValue();
            if (domains != null) {
                for (Pointer domain : domains.getPointerArray(0, count)) {
                    try {
                        result.add(toDto(domain));
                    } finally {
                        lib.virDomainFree(domain);
                    }
                }
                manager.free(domains);
            }
            return result.stream().sorted(Comparator.comparing(v -> v.name)).toList();
        } finally {
            manager.close(conn);
        }
    }

    @Override
    public VmInfoDto getVm(String name) {
        return withDomain(name, this::toDto);
    }

    @Override
    public VmInfoDto createVm(CreateVmRequest request) {
        // 1. 检查是否同名
        Pointer conn = manager.open();
        try {
            Pointer domain = manager.library().virDomainLookupByName(conn, request.name);
            if (domain != null) {
                manager.library().virDomainFree(domain);
                throw new BusinessException("虚拟机 " + request.name + " 已经存在！");
            }
        } catch (Exception e) {
            if (e instanceof BusinessException) {
                throw (BusinessException) e;
            }
        } finally {
            manager.close(conn);
        }

        // 2. 检查模板镜像文件
        if (request.imageName == null || request.imageName.isBlank()) {
            throw new BusinessException("请选择基础系统镜像");
        }
        Path srcImage = imageDir.resolve(request.imageName);
        if (!Files.exists(srcImage)) {
            throw new BusinessException("模板镜像文件不存在：" + srcImage);
        }

        // 3. 复制磁盘映像
        String suffix = "";
        String lowerName = request.imageName.toLowerCase();
        if (lowerName.endsWith(".iso")) {
            suffix = ".iso";
        } else if (lowerName.endsWith(".qcow2")) {
            suffix = ".qcow2";
        } else if (lowerName.endsWith(".img")) {
            suffix = ".img";
        }
        Path targetDisk = imageDir.resolve(request.name + suffix);
        try {
            Files.copy(srcImage, targetDisk, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            throw new BusinessException("拷贝虚拟机模板磁盘失败：" + ex.getMessage());
        }

        // 4. 拼装 XML 配置
        boolean isIso = suffix.equals(".iso");
        String diskXml = isIso ?
            "    <disk type='file' device='cdrom'>\n" +
            "      <driver name='qemu' type='raw'/>\n" +
            "      <source file='" + targetDisk.toAbsolutePath().toString() + "'/>\n" +
            "      <target dev='hda' bus='ide'/>\n" +
            "      <readonly/>\n" +
            "    </disk>\n" :
            "    <disk type='file' device='disk'>\n" +
            "      <driver name='qemu' type='" + (suffix.equals(".qcow2") ? "qcow2" : "raw") + "'/>\n" +
            "      <source file='" + targetDisk.toAbsolutePath().toString() + "'/>\n" +
            "      <target dev='vda' bus='virtio'/>\n" +
            "    </disk>\n";

        String xml = "<domain type='kvm'>\n" +
            "  <name>" + request.name + "</name>\n" +
            "  <uuid>" + java.util.UUID.randomUUID().toString() + "</uuid>\n" +
            "  <memory unit='KiB'>" + (request.memoryMb * 1024) + "</memory>\n" +
            "  <currentMemory unit='KiB'>" + (request.memoryMb * 1024) + "</currentMemory>\n" +
            "  <vcpu placement='static'>" + request.cpuCount + "</vcpu>\n" +
            "  <os>\n" +
            "    <type arch='x86_64' machine='pc-i440fx-rhel10.0.0'>hvm</type>\n" +
            "    <boot dev='" + (isIso ? "cdrom" : "hd") + "'/>\n" +
            "  </os>\n" +
            "  <features>\n" +
            "    <acpi/>\n" +
            "    <apic/>\n" +
            "  </features>\n" +
            "  <cpu mode='host-passthrough' check='none' migratable='on'/>\n" +
            "  <clock offset='utc'>\n" +
            "    <timer name='rtc' tickpolicy='catchup'/>\n" +
            "    <timer name='pit' tickpolicy='delay'/>\n" +
            "    <timer name='hpet' present='no'/>\n" +
            "  </clock>\n" +
            "  <on_poweroff>destroy</on_poweroff>\n" +
            "  <on_reboot>restart</on_reboot>\n" +
            "  <on_crash>destroy</on_crash>\n" +
            "  <devices>\n" +
            "    <emulator>/usr/libexec/qemu-kvm</emulator>\n" +
            diskXml +
            "    <controller type='usb' index='0' model='piix3-uhci'/>\n" +
            "    <controller type='pci' index='0' model='pci-root'/>\n" +
            "    <interface type='network'>\n" +
            "      <source network='" + (request.networkName != null && !request.networkName.isBlank() ? request.networkName : "default") + "'/>\n" +
            "      <model type='virtio'/>\n" +
            "    </interface>\n" +
            "    <graphics type='vnc' port='-1' autoport='yes' listen='0.0.0.0'>\n" +
            "      <listen type='address' address='0.0.0.0'/>\n" +
            "    </graphics>\n" +
            "    <audio id='1' type='none'/>\n" +
            "    <video>\n" +
            "      <model type='vga' vram='16384' heads='1' primary='yes'/>\n" +
            "    </video>\n" +
            "    <memballoon model='virtio'/>\n" +
            "  </devices>\n" +
            "</domain>";

        // 5. 调用 JNA 接口在 libvirt 中定义该虚拟机
        conn = manager.open();
        try {
            Pointer domain = manager.library().virDomainDefineXML(conn, xml, 0);
            if (domain == null) {
                throw new BusinessException("定义虚拟机失败：" + manager.lastErrorMessage());
            }
            try {
                return toDto(domain);
            } finally {
                manager.library().virDomainFree(domain);
            }
        } finally {
            manager.close(conn);
        }
    }

    @Override
    public void startVm(String name) {
        withDomain(name, domain -> {
            check(manager.library().virDomainCreate(domain), "启动虚拟机失败：" + name);
            return null;
        });
    }

    @Override
    public void shutdownVm(String name) {
        withDomain(name, domain -> {
            check(manager.library().virDomainShutdown(domain), "关闭虚拟机失败：" + name);
            return null;
        });
    }

    @Override
    public void destroyVm(String name) {
        withDomain(name, domain -> {
            check(manager.library().virDomainDestroy(domain), "强制关闭虚拟机失败：" + name);
            return null;
        });
    }

    @Override
    public void suspendVm(String name) {
        withDomain(name, domain -> {
            check(manager.library().virDomainSuspend(domain), "暂停虚拟机失败：" + name);
            return null;
        });
    }

    @Override
    public void resumeVm(String name) {
        withDomain(name, domain -> {
            check(manager.library().virDomainResume(domain), "恢复虚拟机失败：" + name);
            return null;
        });
    }

    @Override
    public void deleteVm(String name) {
        withDomain(name, domain -> {
            check(manager.library().virDomainUndefine(domain), "删除虚拟机定义失败：" + name);
            return null;
        });
    }

    private VmInfoDto toDto(Pointer domain) {
        LibvirtLibrary lib = manager.library();
        VmInfoDto dto = new VmInfoDto();
        dto.name = LibvirtUtil.pointerString(lib.virDomainGetName(domain));

        byte[] uuid = new byte[37];
        if (lib.virDomainGetUUIDString(domain, uuid) == 0) {
            dto.uuid = new String(uuid).trim();
        }

        IntByReference stateRef = new IntByReference();
        if (lib.virDomainGetState(domain, stateRef, new IntByReference(), 0) == 0) {
            dto.state = stateName(stateRef.getValue());
        } else {
            dto.state = "未知";
        }

        LibvirtLibrary.VirDomainInfo info = new LibvirtLibrary.VirDomainInfo();
        if (lib.virDomainGetInfo(domain, info) == 0) {
            dto.cpuCount = Short.toUnsignedInt(info.nrVirtCpu);
            dto.memoryMb = (int) (info.memory.longValue() / 1024);
        }

        fillFromXml(domain, dto);

        IntByReference autostart = new IntByReference();
        dto.autostart = lib.virDomainGetAutostart(domain, autostart) == 0 && autostart.getValue() == 1;
        dto.persistent = lib.virDomainIsPersistent(domain) == 1;
        dto.ipAddress = "-";
        dto.description = "libvirt 虚拟机";
        return dto;
    }

    private void fillFromXml(Pointer domain, VmInfoDto dto) {
        LibvirtLibrary lib = manager.library();
        Pointer xmlPointer = lib.virDomainGetXMLDesc(domain, 0);
        if (xmlPointer == null) {
            dto.diskPath = "-";
            dto.networkName = "-";
            return;
        }
        try {
            Document doc = LibvirtUtil.xml(xmlPointer.getString(0, "UTF-8"));
            dto.diskPath = LibvirtUtil.firstAttribute(doc, "source", "file");
            dto.networkName = LibvirtUtil.firstAttribute(doc, "source", "network");
            dto.diskSizeGb = diskSizeGb(dto.diskPath);
            String vncPortStr = LibvirtUtil.firstAttribute(doc, "graphics", "port");
            if (vncPortStr != null && !"-".equals(vncPortStr)) {
                try {
                    dto.vncPort = Integer.parseInt(vncPortStr);
                } catch (NumberFormatException ignored) {}
            }
        } finally {
            manager.free(xmlPointer);
        }
    }

    private int diskSizeGb(String diskPath) {
        if (diskPath == null || diskPath.isBlank() || "-".equals(diskPath)) {
            return 0;
        }
        try {
            return (int) Math.max(1, Math.round(Files.size(Path.of(diskPath)) / 1024.0 / 1024.0 / 1024.0));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private <T> T withDomain(String name, DomainCallback<T> callback) {
        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        Pointer domain = lib.virDomainLookupByName(conn, name);
        if (domain == null) {
            manager.close(conn);
            throw new BusinessException("虚拟机不存在：" + name);
        }
        try {
            return callback.apply(domain);
        } finally {
            lib.virDomainFree(domain);
            manager.close(conn);
        }
    }

    private String stateName(int state) {
        return switch (state) {
            case 1, 2 -> "运行";
            case 3 -> "暂停";
            case 4, 5 -> "关闭";
            case 6 -> "异常";
            default -> "未知";
        };
    }

    private interface DomainCallback<T> {
        T apply(Pointer domain);
    }

    private void check(int code, String message) {
        if (code < 0) {
            throw new BusinessException(message + "：" + manager.lastErrorMessage());
        }
    }
}
