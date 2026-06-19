package com.example.kvm.backend.service;

import com.example.kvm.common.dto.SnapshotInfoDto;
import com.example.kvm.common.request.CreateSnapshotRequest;
import java.util.List;

public interface SnapshotService {
    List<SnapshotInfoDto> listSnapshots(String vmName);
    SnapshotInfoDto createSnapshot(String vmName, CreateSnapshotRequest request);
    void revertSnapshot(String vmName, String snapshotName);
    void deleteSnapshot(String vmName, String snapshotName);
}
