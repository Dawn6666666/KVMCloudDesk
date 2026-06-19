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

`mock` profile 使用内存数据完成 Windows 调试。`libvirt` profile 后续通过 JNA 加载 `/usr/lib64/libvirt.so.0` 并连接 `qemu:///system`。

## 运行截图位置

建议后续保存到：

```text
docs/screenshots/
```
