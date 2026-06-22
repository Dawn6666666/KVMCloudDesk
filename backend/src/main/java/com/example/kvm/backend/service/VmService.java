package com.example.kvm.backend.service;

import com.example.kvm.common.dto.VmInfoDto;
import com.example.kvm.common.request.CreateVmRequest;
import java.util.List;

public interface VmService {
    List<VmInfoDto> listVms();
    VmInfoDto getVm(String name);
    VmInfoDto createVm(CreateVmRequest request);
    void startVm(String name);
    void shutdownVm(String name);
    void destroyVm(String name);
    void suspendVm(String name);
    void resumeVm(String name);
    void rebootVm(String name);
    void deleteVm(String name);
}
