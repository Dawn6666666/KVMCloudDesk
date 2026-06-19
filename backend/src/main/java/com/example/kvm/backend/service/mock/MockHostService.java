package com.example.kvm.backend.service.mock;

import com.example.kvm.backend.service.HostService;
import com.example.kvm.common.dto.HostInfoDto;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
public class MockHostService implements HostService {
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
        return dto;
    }
}
