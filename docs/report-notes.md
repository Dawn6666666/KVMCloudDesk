# 报告素材

## 模块划分

- `common`：共享 DTO、请求、响应。
- `backend`：REST API、profile 切换、mock/libvirt 服务。
- `client-swing`：桌面 GUI、HTTP API 封装、日志展示。
- `client-web`：已新增 Web 界面，复用后端 REST 接口。

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

Web 客户端核心文件：

- `client-web/src/main.ts`
- `client-web/src/router/index.ts`
- `client-web/src/api/http.ts`
- `client-web/src/api/kvm.ts`
- `client-web/src/stores/logStore.ts`
- `client-web/src/layouts/MainLayout.vue`
- `client-web/src/views/DashboardView.vue`
- `client-web/src/views/VmView.vue`
- `client-web/src/views/ImageView.vue`
- `client-web/src/views/NetworkView.vue`
- `client-web/src/views/SnapshotView.vue`
- `client-web/src/views/StorageView.vue`

## 双模式说明

`mock` 配置使用内存数据完成 Windows 调试。`libvirt` 配置通过 JNA 加载 `/usr/lib64/libvirt.so.0` 并连接 `qemu:///system`。

当前真实 libvirt 已支持宿主机信息、虚拟机列表、虚拟机详情、创建虚拟机、删除虚拟机、启动、关机、强制关闭、暂停、恢复、网络列表、网络启停、存储池列表、存储卷列表、快照列表以及快照创建、恢复与删除接口。镜像列表在 libvirt 模式下扫描 `/var/lib/libvirt/images`。

快照测试说明：对于使用 raw 格式磁盘的虚拟机（例如默认的 demo），由于底层存储驱动限制无法支持内部快照，在尝试创建快照时会返回格式不支持的错误，这属于正常现象。目前已在 CentOS 环境中部署了 qcow2 格式磁盘的虚拟机，并成功验证了快照创建、列表查询、恢复回滚以及删除的全部生命周期。

## 运行截图位置

建议保存到：

```text
docs/screenshots/
```
