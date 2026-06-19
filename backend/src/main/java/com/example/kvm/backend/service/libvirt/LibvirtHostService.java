package com.example.kvm.backend.service.libvirt;

import com.example.kvm.backend.service.HostService;
import com.example.kvm.common.dto.HostInfoDto;
import com.sun.jna.Pointer;
import com.sun.jna.ptr.LongByReference;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("libvirt")
public class LibvirtHostService implements HostService {
    private final LibvirtConnectionManager manager;

    public LibvirtHostService(LibvirtConnectionManager manager) {
        this.manager = manager;
    }

    @Override
    public HostInfoDto getHostInfo() {
        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        try {
            HostInfoDto dto = new HostInfoDto();
            Pointer hostname = lib.virConnectGetHostname(conn);
            dto.hostname = LibvirtUtil.pointerString(hostname);
            manager.free(hostname);

            LibvirtLibrary.VirNodeInfo node = new LibvirtLibrary.VirNodeInfo();
            LibvirtUtil.check(lib.virNodeGetInfo(conn, node), "获取宿主机信息失败");
            dto.cpuModel = LibvirtUtil.cString(node.model);
            dto.cpuCount = node.cpus;
            dto.cpuMHz = node.mhz;
            dto.totalMemoryMb = node.memory.longValue() / 1024;
            long freeMemoryBytes = lib.virNodeGetFreeMemory(conn);
            dto.freeMemoryMb = freeMemoryBytes > 0 ? freeMemoryBytes / 1024 / 1024 : 0;
            dto.usedMemoryMb = Math.max(0, dto.totalMemoryMb - dto.freeMemoryMb);
            dto.memoryUsagePercent = dto.totalMemoryMb == 0 ? 0 : (int) (dto.usedMemoryMb * 100 / dto.totalMemoryMb);
            dto.virtualizationType = "KVM";
            dto.kvmEnabled = true;
            dto.connectionUri = manager.uri();

            LongByReference libVersion = new LongByReference();
            if (lib.virConnectGetLibVersion(conn, libVersion) == 0) {
                dto.libvirtVersion = LibvirtUtil.version(libVersion.getValue());
            }
            LongByReference qemuVersion = new LongByReference();
            if (lib.virConnectGetVersion(conn, qemuVersion) == 0) {
                dto.qemuVersion = LibvirtUtil.version(qemuVersion.getValue());
            } else {
                dto.qemuVersion = "未知";
            }
            return dto;
        } finally {
            manager.close(conn);
        }
    }
}
