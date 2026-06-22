export interface ApiResponse<T = any> {
  success: boolean;
  message: string;
  data: T;
}

export interface HostInfoDto {
  hostname: string;
  cpuModel: string;
  cpuCount: number;
  cpuMHz: number;
  totalMemoryMb: number;
  usedMemoryMb: number;
  freeMemoryMb: number;
  memoryUsagePercent: number;
  virtualizationType: string;
  libvirtVersion: string;
  qemuVersion: string;
  kvmEnabled: boolean;
  connectionUri: string;
  cpuUsagePercent: number;
  systemLoadAverage: number;
  cpuSockets: number;
  cpuCores: number;
  cpuThreads: number;
  numaNodes: number;
  osName: string;
  osKernel: string;
  uptime: string;
}

export interface VmInfoDto {
  name: string;
  uuid: string;
  state: string;
  cpuCount: number;
  memoryMb: number;
  diskPath: string;
  diskSizeGb: number;
  networkName: string;
  ipAddress: string;
  autostart: boolean;
  persistent: boolean;
  description: string;
  vncPort?: number;
}

export interface ImageInfoDto {
  name: string;
  path: string;
  format: string;
  sizeGb: number;
  createTime: string;
  description: string;
}

export interface NetworkInfoDto {
  name: string;
  uuid: string;
  active: boolean;
  autostart: boolean;
  bridgeName: string;
  forwardMode: string;
  ipAddress: string;
  dhcpStart: string;
  dhcpEnd: string;
}

export interface SnapshotInfoDto {
  name: string;
  vmName: string;
  createTime: string;
  state: string;
  description: string;
}

export interface StoragePoolInfoDto {
  name: string;
  uuid: string;
  active: boolean;
  autostart: boolean;
  path: string;
  capacityGb: number;
  availableGb: number;
  allocationGb: number;
}

export interface StorageVolumeInfoDto {
  name: string;
  path: string;
  type: string;
  capacityGb: number;
  allocationGb: number;
}

export interface CreateVmRequest {
  name: string;
  cpuCount: number;
  memoryMb: number;
  diskSizeGb: number;
  imageName: string;
  networkName: string;
  description: string;
}

export interface CreateSnapshotRequest {
  name: string;
  description: string;
}

export interface AddImageRequest {
  name: string;
  path: string;
  description: string;
}

export interface CreateNetworkRequest {
  name: string;
  forwardMode: string;
  ipAddress: string;
  netmask: string;
  dhcpEnabled: boolean;
  dhcpStart: string;
  dhcpEnd: string;
}
