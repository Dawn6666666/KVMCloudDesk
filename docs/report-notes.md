# 报告素材

## 模块划分

- `common`：共享 DTO、请求、响应。
- `backend`：REST API、profile 切换、mock/libvirt 服务。
- `client-swing`：桌面 GUI、HTTP API 封装、日志展示。

## 核心类

- `BackendApplication`
- `GlobalExceptionHandler`
- `MockDataStore`
- `BackendApiClient`
- `MainFrame`
- `HostPanel`
- `VmPanel`
- `ImagePanel`
- `NetworkPanel`
- `SnapshotPanel`
- `StoragePanel`

## 双模式说明

`mock` profile 使用内存数据完成 Windows 调试。`libvirt` profile 通过 JNA 加载 `/usr/lib64/libvirt.so.0` 并连接 `qemu:///system`。

当前真实 libvirt 已完成宿主机信息、虚拟机列表、虚拟机详情、启动、关机、强制关闭、暂停、恢复、网络列表、网络启停、存储池列表、存储卷列表、快照列表以及快照创建/恢复/删除接口。镜像列表在 libvirt profile 下扫描 `/var/lib/libvirt/images`。

CentOS demo 虚拟机使用 raw 磁盘，libvirt 不支持 raw 磁盘内部快照，因此实际创建快照时会返回格式限制错误；该限制来自真实环境，不是 mock 或 HTTP 链路问题。

## 运行截图位置

建议后续保存到：

```text
docs/screenshots/
```
