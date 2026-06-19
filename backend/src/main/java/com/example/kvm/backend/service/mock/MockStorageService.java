package com.example.kvm.backend.service.mock;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.StorageService;
import com.example.kvm.common.dto.StoragePoolInfoDto;
import com.example.kvm.common.dto.StorageVolumeInfoDto;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
        return new ArrayList<>(store.volumes.getOrDefault(poolName, List.of()));
    }
}
