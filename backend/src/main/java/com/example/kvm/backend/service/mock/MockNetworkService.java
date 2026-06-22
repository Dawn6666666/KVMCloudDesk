package com.example.kvm.backend.service.mock;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.NetworkService;
import com.example.kvm.common.dto.NetworkInfoDto;
import com.example.kvm.common.request.CreateNetworkRequest;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
public class MockNetworkService implements NetworkService {
    private final MockDataStore store;

    public MockNetworkService(MockDataStore store) {
        this.store = store;
    }

    @Override
    public List<NetworkInfoDto> listNetworks() {
        return store.networks.values().stream().sorted(Comparator.comparing(n -> n.name)).toList();
    }

    @Override
    public void startNetwork(String name) {
        network(name).active = true;
    }

    @Override
    public void stopNetwork(String name) {
        network(name).active = false;
    }

    @Override
    public void createNetwork(CreateNetworkRequest request) {
        if (request.name == null || request.name.isBlank()) {
            throw new BusinessException("网络名称不能为空");
        }
        if (store.networks.containsKey(request.name)) {
            throw new BusinessException("局域网网络 " + request.name + " 已经存在");
        }
        NetworkInfoDto dto = new NetworkInfoDto();
        dto.name = request.name;
        dto.uuid = UUID.randomUUID().toString();
        dto.active = true;
        dto.autostart = true;
        dto.bridgeName = "br-" + (request.name.length() > 10 ? request.name.substring(0, 10) : request.name);
        dto.forwardMode = request.forwardMode != null && !request.forwardMode.isBlank() ? request.forwardMode : "nat";
        dto.ipAddress = request.ipAddress;
        if (request.dhcpEnabled) {
            dto.dhcpStart = request.dhcpStart;
            dto.dhcpEnd = request.dhcpEnd;
        }
        store.networks.put(dto.name, dto);
    }

    @Override
    public void deleteNetwork(String name) {
        if (!store.networks.containsKey(name)) {
            throw new BusinessException("网络不存在：" + name);
        }
        store.networks.remove(name);
    }

    private NetworkInfoDto network(String name) {
        NetworkInfoDto dto = store.networks.get(name);
        if (dto == null) {
            throw new BusinessException("网络不存在：" + name);
        }
        return dto;
    }
}
