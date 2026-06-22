package com.example.kvm.backend.service.mock;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.StorageService;
import com.example.kvm.common.dto.StoragePoolInfoDto;
import com.example.kvm.common.dto.StorageVolumeInfoDto;
import com.example.kvm.common.dto.VmInfoDto;
import com.example.kvm.common.request.CreateVolumeRequest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
public class MockStorageService implements StorageService {
    private final MockDataStore store;

    public MockStorageService(MockDataStore store) {
        this.store = store;
    }

    @Override
    public List<StoragePoolInfoDto> listPools() {
        return store.pools.values().stream().sorted(Comparator.comparing(p -> p.name)).toList();
    }

    @Override
    public List<StorageVolumeInfoDto> listVolumes(String poolName) {
        if (!store.pools.containsKey(poolName)) {
            throw new BusinessException("存储池不存在：" + poolName);
        }
        List<StorageVolumeInfoDto> list = store.volumes.getOrDefault(poolName, List.of());
        Map<String, String> diskVmMap = new HashMap<>();
        for (VmInfoDto vm : store.vms.values()) {
            if (vm.diskPath != null && !vm.diskPath.isBlank()) {
                diskVmMap.put(vm.diskPath, vm.name);
            }
        }
        List<StorageVolumeInfoDto> result = new ArrayList<>();
        for (StorageVolumeInfoDto v : list) {
            StorageVolumeInfoDto dto = new StorageVolumeInfoDto();
            dto.name = v.name;
            dto.path = v.path;
            dto.type = v.type;
            dto.capacityGb = v.capacityGb;
            dto.allocationGb = v.allocationGb;
            dto.vmName = diskVmMap.get(v.path);
            result.add(dto);
        }
        return result.stream().sorted(Comparator.comparing(v -> v.name)).toList();
    }

    @Override
    public StorageVolumeInfoDto createVolume(String poolName, CreateVolumeRequest request) {
        if (!store.pools.containsKey(poolName)) {
            throw new BusinessException("存储池不存在：" + poolName);
        }
        List<StorageVolumeInfoDto> list = store.volumes.computeIfAbsent(poolName, k -> new ArrayList<>());
        if (list.stream().anyMatch(v -> v.name.equals(request.name))) {
            throw new BusinessException("存储卷已存在：" + request.name);
        }
        StorageVolumeInfoDto dto = new StorageVolumeInfoDto();
        dto.name = request.name;
        StoragePoolInfoDto pool = store.pools.get(poolName);
        dto.path = pool.path + "/" + request.name;
        dto.type = "file";
        dto.capacityGb = request.capacityGb;
        dto.allocationGb = 0.0;
        list.add(dto);

        pool.allocationGb += request.capacityGb;
        pool.availableGb = Math.max(0, pool.capacityGb - pool.allocationGb);

        return dto;
    }

    @Override
    public void deleteVolume(String poolName, String volumeName) {
        if (!store.pools.containsKey(poolName)) {
            throw new BusinessException("存储池不存在：" + poolName);
        }
        List<StorageVolumeInfoDto> list = store.volumes.get(poolName);
        if (list != null) {
            Optional<StorageVolumeInfoDto> found = list.stream().filter(v -> v.name.equals(volumeName)).findFirst();
            if (found.isPresent()) {
                StorageVolumeInfoDto vol = found.get();
                Map<String, String> diskVmMap = new HashMap<>();
                for (VmInfoDto vm : store.vms.values()) {
                    if (vm.diskPath != null && vm.diskPath.equals(vol.path)) {
                        diskVmMap.put(vm.diskPath, vm.name);
                    }
                }
                if (diskVmMap.containsKey(vol.path)) {
                    throw new BusinessException("存储卷正被虚拟机使用，无法删除：" + diskVmMap.get(vol.path));
                }
                list.remove(vol);
                StoragePoolInfoDto pool = store.pools.get(poolName);
                pool.allocationGb = Math.max(0, pool.allocationGb - vol.capacityGb);
                pool.availableGb = Math.max(0, pool.capacityGb - pool.allocationGb);
            } else {
                throw new BusinessException("存储卷不存在：" + volumeName);
            }
        }
    }
}
