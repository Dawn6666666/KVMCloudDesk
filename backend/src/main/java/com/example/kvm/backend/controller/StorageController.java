package com.example.kvm.backend.controller;

import com.example.kvm.backend.service.StorageService;
import com.example.kvm.common.dto.StoragePoolInfoDto;
import com.example.kvm.common.dto.StorageVolumeInfoDto;
import com.example.kvm.common.request.CreateVolumeRequest;
import com.example.kvm.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/storage")
public class StorageController {
    private final StorageService storageService;

    public StorageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/pools")
    public ApiResponse<List<StoragePoolInfoDto>> pools() {
        return ApiResponse.ok(storageService.listPools());
    }

    @GetMapping("/pools/{poolName}/volumes")
    public ApiResponse<List<StorageVolumeInfoDto>> volumes(@PathVariable("poolName") String poolName) {
        return ApiResponse.ok(storageService.listVolumes(poolName));
    }

    @PostMapping("/pools/{poolName}/volumes")
    public ApiResponse<StorageVolumeInfoDto> createVolume(@PathVariable("poolName") String poolName,
                                                           @RequestBody CreateVolumeRequest request) {
        return ApiResponse.ok(storageService.createVolume(poolName, request));
    }

    @DeleteMapping("/pools/{poolName}/volumes/{volumeName}")
    public ApiResponse<Void> deleteVolume(@PathVariable("poolName") String poolName,
                                          @PathVariable("volumeName") String volumeName) {
        storageService.deleteVolume(poolName, volumeName);
        return ApiResponse.ok(null);
    }
}
