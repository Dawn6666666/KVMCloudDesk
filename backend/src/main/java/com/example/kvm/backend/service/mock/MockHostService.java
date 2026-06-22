package com.example.kvm.backend.service.mock;

import com.example.kvm.backend.service.HostService;
import com.example.kvm.common.dto.HostInfoDto;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
public class MockHostService implements HostService {
    private long mockRx = 1024L * 1024L * 100L;
    private long mockTx = 1024L * 1024L * 50L;

    @Override
    public HostInfoDto getHostInfo() {
        HostInfoDto dto = new HostInfoDto();
        dto.hostname = "centos";
        dto.cpuModel = "AMD Ryzen 5 3600 6-Core Processor";
        dto.cpuCount = 4;
        dto.cpuMHz = 3593;
        dto.totalMemoryMb = 7647;
        dto.usedMemoryMb = 1700;
        dto.freeMemoryMb = 5947;
        dto.memoryUsagePercent = 22;
        dto.virtualizationType = "KVM";
        dto.libvirtVersion = "11.10.0";
        dto.qemuVersion = "10.1.0";
        dto.kvmEnabled = true;
        dto.connectionUri = "qemu:///system";
        dto.cpuSockets = 1;
        dto.cpuCores = 2;
        dto.cpuThreads = 2;
        dto.numaNodes = 1;
        dto.osName = "Ubuntu Linux 22.04 LTS";
        dto.osKernel = "5.15.0-88-generic";
        dto.uptime = "8天 12小时 30分钟";

        java.lang.management.OperatingSystemMXBean osBean = java.lang.management.ManagementFactory.getOperatingSystemMXBean();
        double systemLoad = osBean.getSystemLoadAverage();
        dto.systemLoadAverage = systemLoad < 0 ? Math.round((Math.random() * 1.5 + 0.1) * 100.0) / 100.0 : Math.round(systemLoad * 100.0) / 100.0;

        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            double cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getCpuLoad();
            if (cpuLoad < 0) {
                cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getSystemCpuLoad();
            }
            dto.cpuUsagePercent = cpuLoad < 0 ? Math.round((Math.random() * 25.0 + 5.0) * 10.0) / 10.0 : Math.round(cpuLoad * 1000.0) / 10.0;
        } else {
            dto.cpuUsagePercent = Math.round((Math.random() * 25.0 + 5.0) * 10.0) / 10.0;
        }

        mockRx += (long) (Math.random() * 1024 * 50);
        mockTx += (long) (Math.random() * 1024 * 30);
        dto.networkRxBytes = mockRx;
        dto.networkTxBytes = mockTx;

        return dto;
    }
}
