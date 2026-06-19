package com.example.kvm.backend.controller;

import com.example.kvm.backend.service.VmService;
import com.example.kvm.common.dto.VmInfoDto;
import com.example.kvm.common.request.CreateVmRequest;
import com.example.kvm.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vms")
public class VmController {
    private final VmService vmService;

    public VmController(VmService vmService) {
        this.vmService = vmService;
    }

    @GetMapping
    public ApiResponse<List<VmInfoDto>> list() {
        return ApiResponse.ok(vmService.listVms());
    }

    @GetMapping("/{name}")
    public ApiResponse<VmInfoDto> get(@PathVariable("name") String name) {
        return ApiResponse.ok(vmService.getVm(name));
    }

    @PostMapping
    public ApiResponse<VmInfoDto> create(@RequestBody CreateVmRequest request) {
        return ApiResponse.ok("虚拟机创建成功", vmService.createVm(request));
    }

    @PostMapping("/{name}/start")
    public ApiResponse<Void> start(@PathVariable("name") String name) {
        vmService.startVm(name);
        return ApiResponse.ok("启动虚拟机成功", null);
    }

    @PostMapping("/{name}/shutdown")
    public ApiResponse<Void> shutdown(@PathVariable("name") String name) {
        vmService.shutdownVm(name);
        return ApiResponse.ok("关闭虚拟机成功", null);
    }

    @PostMapping("/{name}/destroy")
    public ApiResponse<Void> destroy(@PathVariable("name") String name) {
        vmService.destroyVm(name);
        return ApiResponse.ok("强制关闭虚拟机成功", null);
    }

    @PostMapping("/{name}/suspend")
    public ApiResponse<Void> suspend(@PathVariable("name") String name) {
        vmService.suspendVm(name);
        return ApiResponse.ok("暂停虚拟机成功", null);
    }

    @PostMapping("/{name}/resume")
    public ApiResponse<Void> resume(@PathVariable("name") String name) {
        vmService.resumeVm(name);
        return ApiResponse.ok("恢复虚拟机成功", null);
    }

    @DeleteMapping("/{name}")
    public ApiResponse<Void> delete(@PathVariable("name") String name) {
        vmService.deleteVm(name);
        return ApiResponse.ok("删除虚拟机成功", null);
    }
}
