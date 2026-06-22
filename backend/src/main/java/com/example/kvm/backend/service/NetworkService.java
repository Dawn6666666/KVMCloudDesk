package com.example.kvm.backend.service;

import com.example.kvm.common.dto.NetworkInfoDto;
import com.example.kvm.common.request.CreateNetworkRequest;
import java.util.List;

public interface NetworkService {
    List<NetworkInfoDto> listNetworks();
    void startNetwork(String name);
    void stopNetwork(String name);
    void createNetwork(CreateNetworkRequest request);
    void deleteNetwork(String name);
}
