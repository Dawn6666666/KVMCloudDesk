import http from './http';
import type {
  HostInfoDto,
  VmInfoDto,
  ImageInfoDto,
  NetworkInfoDto,
  SnapshotInfoDto,
  StoragePoolInfoDto,
  StorageVolumeInfoDto,
  CreateVmRequest,
  CreateSnapshotRequest,
  AddImageRequest,
  CreateNetworkRequest
} from '@/types/kvm';

export const getHostInfo = (): Promise<HostInfoDto> => {
  return http.get('/host/info');
};

export const getVms = (): Promise<VmInfoDto[]> => {
  return http.get('/vms');
};

export const getVmDetail = (name: string): Promise<VmInfoDto> => {
  return http.get(`/vms/${name}`);
};

export const createVm = (data: CreateVmRequest): Promise<void> => {
  return http.post('/vms', data, {
    headers: { 'X-Action-Description': `创建虚拟机 ${data.name}` }
  });
};

export const startVm = (name: string): Promise<void> => {
  return http.post(`/vms/${name}/start`, null, {
    headers: { 'X-Action-Description': `启动虚拟机 ${name}` }
  });
};

export const shutdownVm = (name: string): Promise<void> => {
  return http.post(`/vms/${name}/shutdown`, null, {
    headers: { 'X-Action-Description': `关闭虚拟机 ${name}` }
  });
};

export const destroyVm = (name: string): Promise<void> => {
  return http.post(`/vms/${name}/destroy`, null, {
    headers: { 'X-Action-Description': `强制关机虚拟机 ${name}` }
  });
};

export const suspendVm = (name: string): Promise<void> => {
  return http.post(`/vms/${name}/suspend`, null, {
    headers: { 'X-Action-Description': `暂停虚拟机 ${name}` }
  });
};

export const resumeVm = (name: string): Promise<void> => {
  return http.post(`/vms/${name}/resume`, null, {
    headers: { 'X-Action-Description': `恢复虚拟机 ${name}` }
  });
};

export const deleteVm = (name: string): Promise<void> => {
  return http.delete(`/vms/${name}`, {
    headers: { 'X-Action-Description': `删除虚拟机 ${name}` }
  });
};

export const getImages = (): Promise<ImageInfoDto[]> => {
  return http.get('/images');
};

export const addImage = (data: AddImageRequest): Promise<void> => {
  return http.post('/images', data, {
    headers: { 'X-Action-Description': `添加系统镜像 ${data.name}` }
  });
};

export const deleteImage = (name: string): Promise<void> => {
  return http.delete(`/images/${name}`, {
    headers: { 'X-Action-Description': `删除系统镜像 ${name}` }
  });
};

export const getNetworks = (): Promise<NetworkInfoDto[]> => {
  return http.get('/networks');
};

export const startNetwork = (name: string): Promise<void> => {
  return http.post(`/networks/${name}/start`, null, {
    headers: { 'X-Action-Description': `启动虚拟网络 ${name}` }
  });
};

export const stopNetwork = (name: string): Promise<void> => {
  return http.post(`/networks/${name}/stop`, null, {
    headers: { 'X-Action-Description': `停止虚拟网络 ${name}` }
  });
};

export const createNetwork = (data: CreateNetworkRequest): Promise<void> => {
  return http.post('/networks', data, {
    headers: { 'X-Action-Description': `创建虚拟网络 ${data.name}` }
  });
};

export const deleteNetwork = (name: string): Promise<void> => {
  return http.delete(`/networks/${name}`, {
    headers: { 'X-Action-Description': `注销虚拟网络 ${name}` }
  });
};

export const getSnapshots = (vmName: string): Promise<SnapshotInfoDto[]> => {
  return http.get(`/vms/${vmName}/snapshots`);
};

export const createSnapshot = (vmName: string, data: CreateSnapshotRequest): Promise<void> => {
  return http.post(`/vms/${vmName}/snapshots`, data, {
    headers: { 'X-Action-Description': `在 ${vmName} 上创建快照 ${data.name}` }
  });
};

export const revertSnapshot = (vmName: string, snapshotName: string): Promise<void> => {
  return http.post(`/vms/${vmName}/snapshots/${snapshotName}/revert`, null, {
    headers: { 'X-Action-Description': `恢复虚拟机 ${vmName} 的快照 ${snapshotName}` }
  });
};

export const deleteSnapshot = (vmName: string, snapshotName: string): Promise<void> => {
  return http.delete(`/vms/${vmName}/snapshots/${snapshotName}`, {
    headers: { 'X-Action-Description': `删除虚拟机 ${vmName} 的快照 ${snapshotName}` }
  });
};

export const getStoragePools = (): Promise<StoragePoolInfoDto[]> => {
  return http.get('/storage/pools');
};

export const getStoragePoolVolumes = (poolName: string): Promise<StorageVolumeInfoDto[]> => {
  return http.get(`/storage/pools/${poolName}/volumes`);
};
