package com.example.kvm.backend.service;

import com.example.kvm.common.dto.NetworkInfoDto;
import java.util.List;

public interface NetworkService {
    List<NetworkInfoDto> listNetworks();
    void startNetwork(String name);
    void stopNetwork(String name);
}
