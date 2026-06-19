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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;

@Service
@Profile("libvirt")
public class LibvirtVmService implements VmService {
    private final LibvirtConnectionManager manager;

    public LibvirtVmService(LibvirtConnectionManager manager) {
        this.manager = manager;
    }

    @Override
    public List<VmInfoDto> listVms() {
        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        PointerByReference domainsRef = new PointerByReference();
        try {
            int count = lib.virConnectListAllDomains(conn, domainsRef, 0);
            LibvirtUtil.check(count, "获取虚拟机列表失败");
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
        throw new BusinessException("libvirt 模式暂未实现创建虚拟机");
    }

    @Override
    public void startVm(String name) {
        withDomain(name, domain -> {
            LibvirtUtil.check(manager.library().virDomainCreate(domain), "启动虚拟机失败：" + name);
            return null;
        });
    }

    @Override
    public void shutdownVm(String name) {
        withDomain(name, domain -> {
            LibvirtUtil.check(manager.library().virDomainShutdown(domain), "关闭虚拟机失败：" + name);
            return null;
        });
    }

    @Override
    public void destroyVm(String name) {
        withDomain(name, domain -> {
            LibvirtUtil.check(manager.library().virDomainDestroy(domain), "强制关闭虚拟机失败：" + name);
            return null;
        });
    }

    @Override
    public void suspendVm(String name) {
        withDomain(name, domain -> {
            LibvirtUtil.check(manager.library().virDomainSuspend(domain), "暂停虚拟机失败：" + name);
            return null;
        });
    }

    @Override
    public void resumeVm(String name) {
        withDomain(name, domain -> {
            LibvirtUtil.check(manager.library().virDomainResume(domain), "恢复虚拟机失败：" + name);
            return null;
        });
    }

    @Override
    public void deleteVm(String name) {
        withDomain(name, domain -> {
            LibvirtUtil.check(manager.library().virDomainUndefine(domain), "删除虚拟机定义失败：" + name);
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
}
