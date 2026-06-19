package com.example.kvm.backend.service.mock;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.NetworkService;
import com.example.kvm.common.dto.NetworkInfoDto;
import java.util.Comparator;
import java.util.List;
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

    private NetworkInfoDto network(String name) {
        NetworkInfoDto dto = store.networks.get(name);
        if (dto == null) {
            throw new BusinessException("网络不存在：" + name);
        }
        return dto;
    }
}
