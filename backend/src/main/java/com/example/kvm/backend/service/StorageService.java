package com.example.kvm.backend.service;

import com.example.kvm.common.dto.StoragePoolInfoDto;
import com.example.kvm.common.dto.StorageVolumeInfoDto;
import com.example.kvm.common.request.CreateVolumeRequest;
import java.util.List;

public interface StorageService {
    List<StoragePoolInfoDto> listPools();
    List<StorageVolumeInfoDto> listVolumes(String poolName);
    StorageVolumeInfoDto createVolume(String poolName, CreateVolumeRequest request);
    void deleteVolume(String poolName, String volumeName);
}
