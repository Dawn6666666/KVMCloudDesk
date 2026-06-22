package com.example.kvm.backend.service.mock;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.VmService;
import com.example.kvm.common.dto.VmInfoDto;
import com.example.kvm.common.request.CreateVmRequest;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("mock")
public class MockVmService implements VmService {
    private final MockDataStore store;

    public MockVmService(MockDataStore store) {
        this.store = store;
    }

    @Override
    public List<VmInfoDto> listVms() {
        return store.vms.values().stream().sorted(Comparator.comparing(v -> v.name)).toList();
    }

    @Override
    public VmInfoDto getVm(String name) {
        VmInfoDto vm = store.vms.get(name);
        if (vm == null) {
            throw new BusinessException("虚拟机不存在：" + name);
        }
        return vm;
    }

    @Override
    public VmInfoDto createVm(CreateVmRequest request) {
        if (request.name == null || request.name.isBlank()) {
            throw new BusinessException("虚拟机名称不能为空");
        }
        if (store.vms.containsKey(request.name)) {
            throw new BusinessException("虚拟机已存在：" + request.name);
        }
        VmInfoDto dto = new VmInfoDto();
        dto.name = request.name;
        dto.uuid = "mock-" + UUID.randomUUID();
        dto.state = "关闭";
        dto.cpuCount = request.cpuCount;
        dto.memoryMb = request.memoryMb;
        dto.diskPath = "/var/lib/libvirt/images/" + request.name + ".qcow2";
        dto.diskSizeGb = request.diskSizeGb;
        dto.networkName = request.networkName;
        dto.ipAddress = "-";
        dto.autostart = false;
        dto.persistent = true;
        dto.description = request.description;
        store.vms.put(dto.name, dto);
        store.snapshots.put(dto.name, new java.util.ArrayList<>());
        return dto;
    }

    @Override
    public void startVm(String name) {
        VmInfoDto vm = getVm(name);
        if ("暂停".equals(vm.state)) {
            throw new BusinessException("暂停状态请使用恢复操作");
        }
        vm.state = "运行";
        vm.vncPort = 5900;
        if ("-".equals(vm.ipAddress)) {
            vm.ipAddress = "192.168.122.120";
        }
    }

    @Override
    public void shutdownVm(String name) {
        VmInfoDto vm = getVm(name);
        if ("运行".equals(vm.state) || "暂停".equals(vm.state)) {
            vm.state = "关闭";
            vm.ipAddress = "-";
            vm.vncPort = null;
        }
    }

    @Override
    public void destroyVm(String name) {
        VmInfoDto vm = getVm(name);
        vm.state = "关闭";
        vm.ipAddress = "-";
        vm.vncPort = null;
    }

    @Override
    public void suspendVm(String name) {
        VmInfoDto vm = getVm(name);
        if (!"运行".equals(vm.state)) {
            throw new BusinessException("只有运行中的虚拟机可以暂停");
        }
        vm.state = "暂停";
    }

    @Override
    public void resumeVm(String name) {
        VmInfoDto vm = getVm(name);
        if (!"暂停".equals(vm.state)) {
            throw new BusinessException("只有暂停中的虚拟机可以恢复");
        }
        vm.state = "运行";
        vm.vncPort = 5900;
    }

    @Override
    public void rebootVm(String name) {
        VmInfoDto vm = getVm(name);
        if (!"运行".equals(vm.state)) {
            throw new BusinessException("只有运行中的虚拟机可以重启");
        }
        vm.state = "运行";
    }

    @Override
    public void deleteVm(String name) {
        if (store.vms.remove(name) == null) {
            throw new BusinessException("虚拟机不存在：" + name);
        }
        store.snapshots.remove(name);
    }
}
