# REST API 参考

所有接口统一返回 `ApiResponse<T>`：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

接口前缀为 `/api`。业务异常返回 HTTP 400，未知异常返回 HTTP 500。

---

## 宿主机

| 方法 | 路径 | 说明 | 响应类型 |
| :--- | :--- | :--- | :--- |
| `GET` | `/host/info` | 获取宿主机信息 | `HostInfoDto` |

## 虚拟机

| 方法 | 路径 | 说明 | 响应类型 |
| :--- | :--- | :--- | :--- |
| `GET` | `/vms` | 列出所有虚拟机 | `List<VmInfoDto>` |
| `GET` | `/vms/{name}` | 获取虚拟机详情 | `VmInfoDto` |
| `POST` | `/vms` | 创建虚拟机 | `VmInfoDto` |
| `POST` | `/vms/{name}/start` | 启动虚拟机 | `Void` |
| `POST` | `/vms/{name}/shutdown` | 请求关机 | `Void` |
| `POST` | `/vms/{name}/destroy` | 强制断电 | `Void` |
| `POST` | `/vms/{name}/suspend` | 暂停虚拟机 | `Void` |
| `POST` | `/vms/{name}/resume` | 恢复虚拟机 | `Void` |
| `DELETE` | `/vms/{name}` | 删除虚拟机（含磁盘） | `Void` |

### 创建虚拟机请求体 `CreateVmRequest`

```json
{
  "name": "my-vm",
  "cpuCount": 2,
  "memoryMb": 1024,
  "diskSizeGb": 10,
  "imageName": "cirros.qcow2",
  "networkName": "default",
  "description": "测试虚拟机"
}
```

## 镜像

| 方法 | 路径 | 说明 | 响应类型 |
| :--- | :--- | :--- | :--- |
| `GET` | `/images` | 列出镜像文件 | `List<ImageInfoDto>` |
| `POST` | `/images` | 添加镜像记录 | `Void` |
| `DELETE` | `/images/{name}` | 删除镜像记录 | `Void` |

### 添加镜像请求体 `AddImageRequest`

```json
{
  "name": "ubuntu-22.04",
  "path": "/var/lib/libvirt/images/ubuntu-22.04.qcow2",
  "description": "Ubuntu 22.04 LTS"
}
```

## 网络

| 方法 | 路径 | 说明 | 响应类型 |
| :--- | :--- | :--- | :--- |
| `GET` | `/networks` | 列出虚拟网络 | `List<NetworkInfoDto>` |
| `POST` | `/networks/{name}/start` | 启动网络 | `Void` |
| `POST` | `/networks/{name}/stop` | 停止网络 | `Void` |

## 快照

| 方法 | 路径 | 说明 | 响应类型 |
| :--- | :--- | :--- | :--- |
| `GET` | `/vms/{vmName}/snapshots` | 列出虚拟机快照 | `List<SnapshotInfoDto>` |
| `POST` | `/vms/{vmName}/snapshots` | 创建快照 | `Void` |
| `POST` | `/vms/{vmName}/snapshots/{snapshotName}/revert` | 恢复到指定快照 | `Void` |
| `DELETE` | `/vms/{vmName}/snapshots/{snapshotName}` | 删除快照 | `Void` |

### 创建快照请求体 `CreateSnapshotRequest`

```json
{
  "name": "snapshot-20260620",
  "description": "升级前快照"
}
```

## 存储

| 方法 | 路径 | 说明 | 响应类型 |
| :--- | :--- | :--- | :--- |
| `GET` | `/storage/pools` | 列出存储池 | `List<StoragePoolInfoDto>` |
| `GET` | `/storage/pools/{poolName}/volumes` | 列出存储卷 | `List<StorageVolumeInfoDto>` |
