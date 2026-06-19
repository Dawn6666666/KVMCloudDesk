# KVM 云平台管理系统

基于 Java 21、Spring Boot、Swing 和后续 Web 前端的 KVM 云平台管理系统。项目采用客户端/服务端架构，Windows 开发阶段使用 `mock` profile 跑通完整链路，CentOS 部署阶段再切换到 `libvirt` profile。

## 模块说明

- `common`：DTO、请求对象、统一响应对象。
- `backend`：Spring Boot REST API，支持 `mock` / `libvirt` profile。
- `client-swing`：Swing 桌面客户端，使用 FlatLaf、MigLayout、Java HttpClient。
- `client-web`：规划新增的 Vue 3 Web 客户端，复用现有 Spring Boot REST API，不直接依赖 libvirt。

## Web 客户端规划

后续将在现有项目上新增 `client-web`，作为和 `client-swing` 并行存在的前端客户端。后端 API、mock/libvirt profile、业务 DTO 均保持复用。

规划技术栈：

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts

开发期建议使用 Vite proxy 将 `/api` 转发到 Spring Boot 后端，避免浏览器跨域问题。详见 `docs/client-web-plan.md`。

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
- 查询网络列表
- 启动/停止网络
- 查询存储池和存储卷
- 查询快照列表
- 创建/恢复/删除快照接口

说明：`shutdown` 是向 guest 发送正常关机请求，真实虚拟机可能因为 ACPI/系统状态没有立即关机；需要立即关闭时使用“强制关闭”。

快照说明：当前 CentOS 环境中的 `demo` 使用 raw 磁盘，libvirt 不支持 raw 磁盘内部快照，因此创建快照会返回明确错误。qcow2 或支持快照的磁盘格式可继续通过快照接口验证。

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
