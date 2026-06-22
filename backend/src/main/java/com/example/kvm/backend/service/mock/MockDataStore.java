package com.example.kvm.backend.service.mock;

import com.example.kvm.common.dto.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("mock")
public class MockDataStore {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public final Map<String, VmInfoDto> vms = new ConcurrentHashMap<>();
    public final Map<String, ImageInfoDto> images = new ConcurrentHashMap<>();
    public final Map<String, NetworkInfoDto> networks = new ConcurrentHashMap<>();
    public final Map<String, List<SnapshotInfoDto>> snapshots = new ConcurrentHashMap<>();
    public final Map<String, StoragePoolInfoDto> pools = new ConcurrentHashMap<>();
    public final Map<String, List<StorageVolumeInfoDto>> volumes = new ConcurrentHashMap<>();

    public MockDataStore() {
        seedVms();
        seedImages();
        seedNetworks();
        seedSnapshots();
        seedStorage();
    }

    public static String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    private void seedVms() {
        vms.put("demo", vm("demo", "mock-demo-uuid", "关闭", 2, 1024,
                "/var/lib/libvirt/images/ubuntu16.04.7.img", 10, "default", "-", "演示虚拟机"));
        vms.put("test-centos", vm("test-centos", "mock-test-centos-uuid", "运行", 1, 512,
                "/var/lib/libvirt/images/test-centos.qcow2", 5, "default", "192.168.122.101", "测试虚拟机"));
    }

    private void seedImages() {
        putImage("ubuntu16.04.7.img", "/var/lib/libvirt/images/ubuntu16.04.7.img", "raw", 10, "Ubuntu 16.04 演示磁盘");
        putImage("cirros.qcow2", "/var/lib/libvirt/images/cirros.qcow2", "qcow2", 1, "轻量测试镜像");
        putImage("centos-test.qcow2", "/var/lib/libvirt/images/centos-test.qcow2", "qcow2", 5, "CentOS 测试镜像");
    }

    private void seedNetworks() {
        NetworkInfoDto dto = new NetworkInfoDto();
        dto.name = "default";
        dto.uuid = "mock-default-network-uuid";
        dto.active = true;
        dto.autostart = true;
        dto.bridgeName = "virbr0";
        dto.forwardMode = "nat";
        dto.ipAddress = "192.168.122.1";
        dto.netmask = "255.255.255.0";
        dto.dhcpStart = "192.168.122.2";
        dto.dhcpEnd = "192.168.122.254";
        networks.put(dto.name, dto);
    }

    private void seedSnapshots() {
        snapshots.put("demo", new ArrayList<>(List.of(
                snapshot("snapshot-001", "demo", "关闭", "演示快照 001", false),
                snapshot("snapshot-002", "demo", "关闭", "演示快照 002", true))));
        snapshots.put("test-centos", new ArrayList<>(List.of(
                snapshot("base-snapshot", "test-centos", "运行", "基础快照", true))));
    }

    private void seedStorage() {
        StoragePoolInfoDto pool = new StoragePoolInfoDto();
        pool.name = "default";
        pool.uuid = "mock-default-pool-uuid";
        pool.active = true;
        pool.autostart = true;
        pool.path = "/var/lib/libvirt/images";
        pool.capacityGb = 47;
        pool.availableGb = 18;
        pool.allocationGb = 29;
        pools.put(pool.name, pool);
        volumes.put("default", new ArrayList<>(List.of(
                volume("ubuntu16.04.7.img", "/var/lib/libvirt/images/ubuntu16.04.7.img", "file", 10, 10),
                volume("test-centos.qcow2", "/var/lib/libvirt/images/test-centos.qcow2", "file", 5, 2.2),
                volume("cirros.qcow2", "/var/lib/libvirt/images/cirros.qcow2", "file", 1, 0.1))));
    }

    private void putImage(String name, String path, String format, double sizeGb, String description) {
        ImageInfoDto dto = new ImageInfoDto();
        dto.name = name;
        dto.path = path;
        dto.format = format;
        dto.sizeGb = sizeGb;
        dto.physicalSizeGb = sizeGb * 0.8;
        dto.exists = true;
        dto.createTime = now();
        dto.description = description;
        images.put(name, dto);
    }

    private static VmInfoDto vm(String name, String uuid, String state, int cpu, int memory, String diskPath,
                                int diskSize, String network, String ip, String description) {
        VmInfoDto dto = new VmInfoDto();
        dto.name = name;
        dto.uuid = uuid;
        dto.state = state;
        dto.cpuCount = cpu;
        dto.memoryMb = memory;
        dto.diskPath = diskPath;
        dto.diskSizeGb = diskSize;
        dto.networkName = network;
        dto.ipAddress = ip;
        dto.autostart = false;
        dto.persistent = true;
        dto.description = description;
        dto.vncPort = "运行".equals(state) ? 5900 : null;
        return dto;
    }

    private static SnapshotInfoDto snapshot(String name, String vmName, String state, String description, boolean current) {
        SnapshotInfoDto dto = new SnapshotInfoDto();
        dto.name = name;
        dto.vmName = vmName;
        dto.createTime = now();
        dto.state = state;
        dto.description = description;
        dto.current = current;
        return dto;
    }

    private static StorageVolumeInfoDto volume(String name, String path, String type, double capacity, double allocation) {
        StorageVolumeInfoDto dto = new StorageVolumeInfoDto();
        dto.name = name;
        dto.path = path;
        dto.type = type;
        dto.capacityGb = capacity;
        dto.allocationGb = allocation;
        return dto;
    }
}
