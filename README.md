# KVM 云平台管理系统

基于 Java 21、Spring Boot、Swing 和 Vue 3 Web 前端的 KVM 云平台管理系统。项目采用 C/S 架构，后端通过 REST API 提供服务，Windows 开发阶段使用 `mock` 配置测试链路，CentOS 部署阶段切换到 `libvirt` 配置以接入 KVM 虚拟化环境。

## 技术栈

| 模块 | 技术 |
| :--- | :--- |
| `common` | Java 21、Jackson 2.17.2 |
| `backend` | Spring Boot 3.3.6、JNA 5.14.0、libvirt C API |
| `client-swing` | Java 21 Swing、FlatLaf 3.5.4、MigLayout 11.4.2、Java HttpClient |
| `client-web` | Vue 3.5、TypeScript 6.0、Vite 8.0、Element Plus 2.14、Pinia 3.0、ECharts 6.1、Axios 1.18 |

## 模块说明

- `common`：DTO、请求对象与统一响应对象 `ApiResponse<T>`。
- `backend`：Spring Boot REST API，支持 `mock` 与 `libvirt` 双配置模式。libvirt 模式下通过 JNA 映射约 57 个 libvirt C 函数与 4 个 JNA 结构体，不依赖 virsh。
- `client-swing`：Swing 桌面客户端，使用 FlatLaf 明暗主题切换、MigLayout 布局与 SwingWorker 异步调用。
- `client-web`：基于 Vue 3、TypeScript、Element Plus、Pinia 与 ECharts 的 Web 客户端，主色调为 `#ca6a1f`，提供控制面板图表、全局操作日志追踪面板与关机轮询进度指示。

## Web 客户端运行与开发

Web 客户端提供宿主机参数展示、虚拟机生命周期管理、快照创建与回滚、桥接网络管理、存储卷查看及 ECharts 仪表盘等功能。

### 运行开发服务器

1. 确保 Spring Boot 后端（如 mock 模式）正常启动在 8080 端口。
2. 进入 `client-web` 目录，执行以下命令安装依赖并启动服务：

```bash
cd client-web
npm install
npm run dev
```

3. 启动后通过浏览器访问命令行输出的调试地址（默认为 `http://localhost:5173`）即可。

> **注意**：`vite.config.ts` 中 `/api` 代理默认指向 CentOS 远程后端 `http://192.168.61.130:8080`。若要在本地 mock 模式下开发，需将 `target` 改为 `http://127.0.0.1:8080`。

### 生产编译打包

在 `client-web` 目录下执行以下命令：

```bash
npm run build
```

打包生成的文件位于 `client-web/dist`。

## Windows 开发运行

由于开发机默认 JDK 版本与 Maven 环境配置差异，建议优先参考 [docs/quickstart.md](file:///D:/Code/Other/kvm/docs/quickstart.md) 中提供的一键构建指令。

常规运行命令：
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

当前 `libvirt` 配置已接入真实虚拟化环境：

- 连接宿主机：配置 CentOS 本地回环 `/etc/hosts`，降低获取宿主机名时的域名解析延迟，使操作延迟维持在 0.15 秒内。
- 获取宿主机信息：排除多余 QEMU 程序的干扰，能够读取 QEMU 10.1.0 版本信息。
- 虚拟机创建与定义：支持基于已有系统镜像或光盘创建虚拟机。
- 虚拟机删除：支持清除定义并同步删除磁盘映像文件。
- 获取虚拟机列表与状态。
- 获取虚拟机详情。
- 启动虚拟机：Web 端提供步骤日志与防抖处理。
- 关闭虚拟机：Web 端包含进度指示、状态轮询与超时降级机制。
- 强制关闭虚拟机：支持在关机超时或异常时，在弹窗中选择强制断电。
- 暂停与恢复虚拟机：Web 端包含操作日志与状态记录。
- 查询虚拟网络列表与状态切换：支持启动和停止网络。
- 查询存储池与存储卷列表。
- 快照生命周期管理：支持快照创建、列表查询、恢复与删除。

说明：关闭虚拟机操作是向虚拟机发送正常关机信号（ACPI），若虚拟机内部系统未运行相关服务，可能无法立即关闭。若超时无响应，可在弹窗中选择强制断电以断开电源。

快照说明：使用 raw 磁盘格式的虚拟机受底层存储驱动限制，不支持创建快照，调用接口会返回格式不支持的错误。目前已在 CentOS 环境中新增部署 qcow2 磁盘格式的测试虚拟机，验证了快照的创建、查询、恢复与删除操作。

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
- 虚拟机点击“关机”后界面长时间无反应，或提示“关机超时”的解决方法？
  安全关机操作在底层向虚拟机内系统发送的是 ACPI 关机信号。若虚拟机内没有响应或系统不支持，将无法自动关机并在 22.5 秒后触发超时提示。此时可在前端进度弹窗中选择强制断电（对应后端的销毁接口），即可对虚拟机进行强行关机。
