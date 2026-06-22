package com.example.kvm.backend.service.libvirt;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.StorageService;
import com.example.kvm.common.dto.StoragePoolInfoDto;
import com.example.kvm.common.dto.StorageVolumeInfoDto;
import com.example.kvm.common.request.CreateVolumeRequest;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Service
@Profile("libvirt")
public class LibvirtStorageService implements StorageService {
    private final LibvirtConnectionManager manager;

    public LibvirtStorageService(LibvirtConnectionManager manager) {
        this.manager = manager;
    }

    @Override
    public List<StoragePoolInfoDto> listPools() {
        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        PointerByReference poolsRef = new PointerByReference();
        try {
            int count = lib.virConnectListAllStoragePools(conn, poolsRef, 0);
            check(count, "获取存储池列表失败");
            List<StoragePoolInfoDto> result = new ArrayList<>();
            Pointer pools = poolsRef.getValue();
            if (pools != null) {
                for (Pointer pool : pools.getPointerArray(0, count)) {
                    try {
                        result.add(toPoolDto(pool));
                    } finally {
                        lib.virStoragePoolFree(pool);
                    }
                }
                manager.free(pools);
            }
            return result.stream().sorted(Comparator.comparing(p -> p.name)).toList();
        } finally {
            manager.close(conn);
        }
    }

    private Map<String, String> getDiskVmMap() {
        Map<String, String> map = new HashMap<>();
        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        PointerByReference domainsRef = new PointerByReference();
        try {
            int count = lib.virConnectListAllDomains(conn, domainsRef, 0);
            if (count > 0) {
                Pointer domains = domainsRef.getValue();
                if (domains != null) {
                    for (Pointer domain : domains.getPointerArray(0, count)) {
                        try {
                            String vmName = LibvirtUtil.pointerString(lib.virDomainGetName(domain));
                            Pointer xmlPointer = lib.virDomainGetXMLDesc(domain, 0);
                            if (xmlPointer != null) {
                                try {
                                    Document doc = LibvirtUtil.xml(xmlPointer.getString(0, "UTF-8"));
                                    NodeList disks = doc.getElementsByTagName("disk");
                                    for (int i = 0; i < disks.getLength(); i++) {
                                        if (disks.item(i) instanceof Element diskEl) {
                                            NodeList sources = diskEl.getElementsByTagName("source");
                                            if (sources.getLength() > 0 && sources.item(0) instanceof Element sourceEl) {
                                                if (sourceEl.hasAttribute("file")) {
                                                    String file = sourceEl.getAttribute("file");
                                                    if (file != null && !file.isBlank()) {
                                                        map.put(file, vmName);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } finally {
                                    manager.free(xmlPointer);
                                }
                            }
                        } catch (Exception ignored) {
                        } finally {
                            lib.virDomainFree(domain);
                        }
                    }
                    manager.free(domains);
                }
            }
        } catch (Exception ignored) {
        } finally {
            manager.close(conn);
        }
        return map;
    }

    @Override
    public List<StorageVolumeInfoDto> listVolumes(String poolName) {
        Map<String, String> diskVmMap = getDiskVmMap();
        return withPool(poolName, pool -> {
            LibvirtLibrary lib = manager.library();
            PointerByReference volumesRef = new PointerByReference();
            int count = lib.virStoragePoolListAllVolumes(pool, volumesRef, 0);
            check(count, "获取存储卷列表失败：" + poolName);
            List<StorageVolumeInfoDto> result = new ArrayList<>();
            Pointer volumes = volumesRef.getValue();
            if (volumes != null) {
                for (Pointer volume : volumes.getPointerArray(0, count)) {
                    try {
                        StorageVolumeInfoDto dto = toVolumeDto(volume);
                        dto.vmName = diskVmMap.get(dto.path);
                        result.add(dto);
                    } finally {
                        lib.virStorageVolFree(volume);
                    }
                }
                manager.free(volumes);
            }
            return result.stream().sorted(Comparator.comparing(v -> v.name)).toList();
        });
    }

    @Override
    public StorageVolumeInfoDto createVolume(String poolName, CreateVolumeRequest request) {
        return withPool(poolName, pool -> {
            LibvirtLibrary lib = manager.library();
            String format = request.format != null ? request.format.toLowerCase() : "qcow2";
            long bytes = (long) (request.capacityGb * 1024 * 1024 * 1024);
            String xml = "<volume>\n" +
                         "  <name>" + request.name + "</name>\n" +
                         "  <capacity unit='bytes'>" + bytes + "</capacity>\n" +
                         "  <target>\n" +
                         "    <format type='" + format + "'/>\n" +
                         "  </target>\n" +
                         "</volume>";
            Pointer vol = lib.virStorageVolCreateXML(pool, xml, 0);
            if (vol == null) {
                throw new BusinessException("创建存储卷失败：" + manager.lastErrorMessage());
            }
            try {
                return toVolumeDto(vol);
            } finally {
                lib.virStorageVolFree(vol);
            }
        });
    }

    @Override
    public void deleteVolume(String poolName, String volumeName) {
        withPool(poolName, pool -> {
            LibvirtLibrary lib = manager.library();
            Pointer vol = lib.virStorageVolLookupByName(pool, volumeName);
            if (vol == null) {
                throw new BusinessException("存储卷不存在：" + volumeName);
            }
            try {
                Map<String, String> diskVmMap = getDiskVmMap();
                StorageVolumeInfoDto dto = toVolumeDto(vol);
                if (diskVmMap.containsKey(dto.path)) {
                    throw new BusinessException("存储卷正被虚拟机使用，无法删除：" + diskVmMap.get(dto.path));
                }
                check(lib.virStorageVolDelete(vol, 0), "删除存储卷失败：" + volumeName);
                return null;
            } finally {
                lib.virStorageVolFree(vol);
            }
        });
    }

    private StoragePoolInfoDto toPoolDto(Pointer pool) {
        LibvirtLibrary lib = manager.library();
        StoragePoolInfoDto dto = new StoragePoolInfoDto();
        dto.name = LibvirtUtil.pointerString(lib.virStoragePoolGetName(pool));
        byte[] uuid = new byte[37];
        if (lib.virStoragePoolGetUUIDString(pool, uuid) == 0) {
            dto.uuid = LibvirtUtil.cString(uuid);
        }
        dto.active = lib.virStoragePoolIsActive(pool) == 1;
        IntByReference autostart = new IntByReference();
        dto.autostart = lib.virStoragePoolGetAutostart(pool, autostart) == 0 && autostart.getValue() == 1;
        LibvirtLibrary.VirStoragePoolInfo info = new LibvirtLibrary.VirStoragePoolInfo();
        if (lib.virStoragePoolGetInfo(pool, info) == 0) {
            dto.capacityGb = LibvirtUtil.bytesToGb(info.capacity);
            dto.allocationGb = LibvirtUtil.bytesToGb(info.allocation);
            dto.availableGb = LibvirtUtil.bytesToGb(info.available);
        }
        Pointer xmlPointer = lib.virStoragePoolGetXMLDesc(pool, 0);
        if (xmlPointer != null) {
            try {
                Document doc = LibvirtUtil.xml(xmlPointer.getString(0, "UTF-8"));
                dto.path = LibvirtUtil.firstText(doc, "path");
            } finally {
                manager.free(xmlPointer);
            }
        }
        return dto;
    }

    private StorageVolumeInfoDto toVolumeDto(Pointer volume) {
        LibvirtLibrary lib = manager.library();
        StorageVolumeInfoDto dto = new StorageVolumeInfoDto();
        dto.name = LibvirtUtil.pointerString(lib.virStorageVolGetName(volume));
        Pointer path = lib.virStorageVolGetPath(volume);
        dto.path = LibvirtUtil.pointerString(path);
        manager.free(path);
        LibvirtLibrary.VirStorageVolInfo info = new LibvirtLibrary.VirStorageVolInfo();
        if (lib.virStorageVolGetInfo(volume, info) == 0) {
            dto.type = volumeType(info.type);
            dto.capacityGb = LibvirtUtil.bytesToGb(info.capacity);
            dto.allocationGb = LibvirtUtil.bytesToGb(info.allocation);
        }
        return dto;
    }

    private <T> T withPool(String name, PoolCallback<T> callback) {
        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        Pointer pool = lib.virStoragePoolLookupByName(conn, name);
        if (pool == null) {
            manager.close(conn);
            throw new BusinessException("存储池不存在：" + name);
        }
        try {
            return callback.apply(pool);
        } finally {
            lib.virStoragePoolFree(pool);
            manager.close(conn);
        }
    }

    private String volumeType(int type) {
        return switch (type) {
            case 0 -> "file";
            case 1 -> "block";
            case 2 -> "dir";
            case 3 -> "network";
            case 4 -> "netdir";
            default -> "unknown";
        };
    }

    private interface PoolCallback<T> {
        T apply(Pointer pool);
    }

    private void check(int code, String message) {
        if (code < 0) {
            throw new BusinessException(message + "：" + manager.lastErrorMessage());
        }
    }
}
