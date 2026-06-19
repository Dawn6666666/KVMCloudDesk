package com.example.kvm.backend.service.mock;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.SnapshotService;
import com.example.kvm.common.dto.SnapshotInfoDto;
import com.example.kvm.common.request.CreateSnapshotRequest;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
public class MockSnapshotService implements SnapshotService {
    private final MockDataStore store;

    public MockSnapshotService(MockDataStore store) {
        this.store = store;
    }

    @Override
    public List<SnapshotInfoDto> listSnapshots(String vmName) {
        requireVm(vmName);
        return new ArrayList<>(store.snapshots.getOrDefault(vmName, List.of()));
    }

    @Override
    public SnapshotInfoDto createSnapshot(String vmName, CreateSnapshotRequest request) {
        requireVm(vmName);
        if (request.name == null || request.name.isBlank()) {
            throw new BusinessException("快照名称不能为空");
        }
        SnapshotInfoDto dto = new SnapshotInfoDto();
        dto.name = request.name;
        dto.vmName = vmName;
        dto.createTime = MockDataStore.now();
        dto.state = store.vms.get(vmName).state;
        dto.description = request.description;
        store.snapshots.computeIfAbsent(vmName, key -> new ArrayList<>()).add(dto);
        return dto;
    }

    @Override
    public void revertSnapshot(String vmName, String snapshotName) {
        findSnapshot(vmName, snapshotName);
    }

    @Override
    public void deleteSnapshot(String vmName, String snapshotName) {
        List<SnapshotInfoDto> list = store.snapshots.get(vmName);
        if (list == null || !list.removeIf(s -> s.name.equals(snapshotName))) {
            throw new BusinessException("快照不存在：" + snapshotName);
        }
    }

    private void requireVm(String vmName) {
        if (!store.vms.containsKey(vmName)) {
            throw new BusinessException("虚拟机不存在：" + vmName);
        }
    }

    private void findSnapshot(String vmName, String snapshotName) {
        requireVm(vmName);
        boolean exists = store.snapshots.getOrDefault(vmName, List.of()).stream().anyMatch(s -> s.name.equals(snapshotName));
        if (!exists) {
            throw new BusinessException("快照不存在：" + snapshotName);
        }
    }
}
