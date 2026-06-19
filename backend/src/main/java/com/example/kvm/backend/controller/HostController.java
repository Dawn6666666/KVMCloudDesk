package com.example.kvm.backend.controller;

import com.example.kvm.backend.service.HostService;
import com.example.kvm.common.dto.HostInfoDto;
import com.example.kvm.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/host")
public class HostController {
    private final HostService hostService;

    public HostController(HostService hostService) {
        this.hostService = hostService;
    }

    @GetMapping("/info")
    public ApiResponse<HostInfoDto> info() {
        return ApiResponse.ok(hostService.getHostInfo());
    }
}
