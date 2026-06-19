# KVM 云平台管理系统

基于 Java 21、Spring Boot 和 Swing 的 KVM 云平台管理系统。项目采用客户端/服务端架构，Windows 开发阶段使用 `mock` profile 跑通完整链路，CentOS 部署阶段再切换到 `libvirt` profile。

## 模块说明

- `common`：DTO、请求对象、统一响应对象。
- `backend`：Spring Boot REST API，支持 `mock` / `libvirt` profile。
- `client-swing`：Swing 桌面客户端，使用 FlatLaf、MigLayout、Java HttpClient。

## Windows 开发运行

```bash
mvn clean package
java -jar backend/target/kvm-cloud-backend.jar --spring.profiles.active=mock
java -jar client-swing/target/kvm-cloud-client.jar
```

客户端默认读取 `client.properties`：

```properties
backend.url=http://127.0.0.1:8080
```

也可通过启动参数覆盖：

```bash
java -jar client-swing/target/kvm-cloud-client.jar --backend.url=http://192.168.61.130:8080
```

## CentOS 部署

后端部署到 CentOS Stream 10 后使用：

```bash
java -jar kvm-cloud-backend.jar \
  --spring.profiles.active=libvirt \
  --server.address=0.0.0.0 \
  --server.port=8080
```

当前 `libvirt` profile 已实现最小真实链路：

- 连接 `qemu:///system`
- 获取宿主机信息
- 列出真实虚拟机
- 获取虚拟机详情
- 启动虚拟机
- 请求虚拟机关机
- 强制关闭虚拟机
- 暂停虚拟机
- 恢复虚拟机

说明：`shutdown` 是向 guest 发送正常关机请求，真实虚拟机可能因为 ACPI/系统状态没有立即关机；需要立即关闭时使用“强制关闭”。

## API 列表

- `GET /api/host/info`
- `GET /api/vms`
- `GET /api/vms/{name}`
- `POST /api/vms`
- `POST /api/vms/{name}/start`
- `POST /api/vms/{name}/shutdown`
- `POST /api/vms/{name}/destroy`
- `POST /api/vms/{name}/suspend`
- `POST /api/vms/{name}/resume`
- `DELETE /api/vms/{name}`
- `GET /api/images`
- `POST /api/images`
- `DELETE /api/images/{name}`
- `GET /api/networks`
- `POST /api/networks/{name}/start`
- `POST /api/networks/{name}/stop`
- `GET /api/vms/{vmName}/snapshots`
- `POST /api/vms/{vmName}/snapshots`
- `POST /api/vms/{vmName}/snapshots/{snapshotName}/revert`
- `DELETE /api/vms/{vmName}/snapshots/{snapshotName}`
- `GET /api/storage/pools`
- `GET /api/storage/pools/{poolName}/volumes`

## 禁止 virsh 说明

项目业务代码禁止通过 `Runtime.exec`、`ProcessBuilder` 或其他方式调用 `virsh`。`virsh` 仅可用于人工环境诊断，不写入业务实现。

## 常见问题

- Windows 上请使用 `mock` profile，不要启动 `libvirt` profile。
- Windows 本地不需要安装 libvirt，也不会加载 `/usr/lib64/libvirt.so.0`。
- Swing 客户端只通过 HTTP JSON 访问后端，不直接依赖 libvirt。
