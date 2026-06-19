package com.example.kvm.backend.controller;

import com.example.kvm.backend.service.SnapshotService;
import com.example.kvm.common.dto.SnapshotInfoDto;
import com.example.kvm.common.request.CreateSnapshotRequest;
import com.example.kvm.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vms/{vmName}/snapshots")
public class SnapshotController {
    private final SnapshotService snapshotService;

    public SnapshotController(SnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @GetMapping
    public ApiResponse<List<SnapshotInfoDto>> list(@PathVariable("vmName") String vmName) {
        return ApiResponse.ok(snapshotService.listSnapshots(vmName));
    }

    @PostMapping
    public ApiResponse<SnapshotInfoDto> create(@PathVariable("vmName") String vmName, @RequestBody CreateSnapshotRequest request) {
        return ApiResponse.ok("快照创建成功", snapshotService.createSnapshot(vmName, request));
    }

    @PostMapping("/{snapshotName}/revert")
    public ApiResponse<Void> revert(@PathVariable("vmName") String vmName, @PathVariable("snapshotName") String snapshotName) {
        snapshotService.revertSnapshot(vmName, snapshotName);
        return ApiResponse.ok("快照恢复成功", null);
    }

    @DeleteMapping("/{snapshotName}")
    public ApiResponse<Void> delete(@PathVariable("vmName") String vmName, @PathVariable("snapshotName") String snapshotName) {
        snapshotService.deleteSnapshot(vmName, snapshotName);
        return ApiResponse.ok("快照删除成功", null);
    }
}
