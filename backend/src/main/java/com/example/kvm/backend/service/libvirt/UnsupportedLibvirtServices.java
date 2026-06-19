package com.example.kvm.backend.service.libvirt;

import com.example.kvm.backend.exception.BusinessException;
import com.example.kvm.backend.service.*;
import com.example.kvm.common.dto.*;
import com.example.kvm.common.request.*;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("libvirt")
public class UnsupportedLibvirtServices implements HostService, VmService, ImageService, NetworkService, SnapshotService, StorageService {
    private BusinessException todo() {
        return new BusinessException("libvirt profile 骨架已隔离，当前阶段请使用 mock profile");
    }

    public HostInfoDto getHostInfo() { throw todo(); }
    public List<VmInfoDto> listVms() { throw todo(); }
    public VmInfoDto getVm(String name) { throw todo(); }
    public VmInfoDto createVm(CreateVmRequest request) { throw todo(); }
    public void startVm(String name) { throw todo(); }
    public void shutdownVm(String name) { throw todo(); }
    public void destroyVm(String name) { throw todo(); }
    public void suspendVm(String name) { throw todo(); }
    public void resumeVm(String name) { throw todo(); }
    public void deleteVm(String name) { throw todo(); }
    public List<ImageInfoDto> listImages() { throw todo(); }
    public ImageInfoDto addImage(AddImageRequest request) { throw todo(); }
    public void deleteImage(String name) { throw todo(); }
    public List<NetworkInfoDto> listNetworks() { throw todo(); }
    public void startNetwork(String name) { throw todo(); }
    public void stopNetwork(String name) { throw todo(); }
    public List<SnapshotInfoDto> listSnapshots(String vmName) { throw todo(); }
    public SnapshotInfoDto createSnapshot(String vmName, CreateSnapshotRequest request) { throw todo(); }
    public void revertSnapshot(String vmName, String snapshotName) { throw todo(); }
    public void deleteSnapshot(String vmName, String snapshotName) { throw todo(); }
    public List<StoragePoolInfoDto> listPools() { throw todo(); }
    public List<StorageVolumeInfoDto> listVolumes(String poolName) { throw todo(); }
}
