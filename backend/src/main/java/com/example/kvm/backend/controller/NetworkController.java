package com.example.kvm.backend.controller;

import com.example.kvm.backend.service.NetworkService;
import com.example.kvm.common.dto.NetworkInfoDto;
import com.example.kvm.common.request.CreateNetworkRequest;
import com.example.kvm.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/networks")
public class NetworkController {
    private final NetworkService networkService;

    public NetworkController(NetworkService networkService) {
        this.networkService = networkService;
    }

    @GetMapping
    public ApiResponse<List<NetworkInfoDto>> list() {
        return ApiResponse.ok(networkService.listNetworks());
    }

    @PostMapping("/{name}/start")
    public ApiResponse<Void> start(@PathVariable("name") String name) {
        networkService.startNetwork(name);
        return ApiResponse.ok("启动网络成功", null);
    }

    @PostMapping("/{name}/stop")
    public ApiResponse<Void> stop(@PathVariable("name") String name) {
        networkService.stopNetwork(name);
        return ApiResponse.ok("停止网络成功", null);
    }

    @PostMapping
    public ApiResponse<Void> create(@RequestBody CreateNetworkRequest request) {
        networkService.createNetwork(request);
        return ApiResponse.ok("创建网络成功", null);
    }

    @DeleteMapping("/{name}")
    public ApiResponse<Void> delete(@PathVariable("name") String name) {
        networkService.deleteNetwork(name);
        return ApiResponse.ok("注销网络成功", null);
    }
}
