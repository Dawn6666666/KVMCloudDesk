# KVM 云平台管理系统

基于 Java 21、Spring Boot、Swing 和后续 Web 前端的 KVM 云平台管理系统。项目采用客户端/服务端架构，Windows 开发阶段使用 `mock` profile 跑通完整链路，CentOS 部署阶段再切换到 `libvirt` profile。

## 模块说明

- `common`：DTO、请求对象、统一响应对象。
- `backend`：Spring Boot REST API，支持 `mock` / `libvirt` profile。
- `client-swing`：Swing 桌面客户端，使用 FlatLaf、MigLayout、Java HttpClient。
- `client-web`：基于 Vue 3 + Vite + TypeScript + Element Plus + Pinia + ECharts 的 Web 客户端，复用现有 Spring Boot REST API，不直接依赖 libvirt。

## Web 客户端运行与开发

Web 客户端已全部开发完成，提供宿主机参数展示、虚拟机全状态生命周期管理、快照回滚拍摄、桥接网络管理、存储卷明细查看及 ECharts 仪表盘等功能。

### 运行开发服务器

1. 确保 Spring Boot 后端（如 mock 模式）正常启动在 8080 端口。
2. 进入 `client-web` 目录，执行以下命令安装依赖并启动服务：

```bash
cd client-web
npm install
npm run dev
```

3. 启动后通过浏览器访问命令行输出的调试地址（默认为 `http://localhost:5173`）即可。

### 生产编译打包

在 `client-web` 目录下执行以下命令：

```bash
npm run build
```

打包生成的文件位于 `client-web/dist`。

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

当前 `libvirt` profile 已实现完整真实链路：

- 连接宿主机（已优化获取宿主机名逻辑，移除了导致 15 秒反向解析超时的原生调用，将响应时间缩短至 0.45 秒内）
- 获取宿主机信息
- 虚拟机创建与定义（支持基于已部署的系统镜像或光盘进行创建）
- 虚拟机删除（支持清除定义并同步物理删除磁盘映像文件）
- 列出真实虚拟机及状态展示
- 获取虚拟机详情
- 启动虚拟机
- 请求虚拟机关机
- 强制关闭虚拟机
- 暂停虚拟机
- 恢复虚拟机
- 查询虚拟网络列表与状态切换（启动/停止网络）
- 查询存储池与存储卷列表
- 快照全生命周期管理（创建快照、快照列表查询、恢复回滚、彻底删除）

说明：`shutdown` 是向 guest 发送正常关机请求，真实虚拟机可能因为系统状态没有立即关机；需要立即关闭时使用“强制关闭”。

快照说明：对于使用 raw 磁盘格式的默认虚拟机（例如 demo），由于底层存储驱动限制无法支持内部快照，相关快照接口会返回格式不支持的业务错误。目前已在 CentOS 环境中通过新增部署 qcow2 磁盘格式的测试虚拟机（例如 cirros-test-052），成功验证了快照创建、列表查询、恢复回滚以及删除的全部生命周期。

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
