package com.example.kvm.backend.service;

import com.example.kvm.common.dto.StoragePoolInfoDto;
import com.example.kvm.common.dto.StorageVolumeInfoDto;
import java.util.List;

public interface StorageService {
    List<StoragePoolInfoDto> listPools();
    List<StorageVolumeInfoDto> listVolumes(String poolName);
}
