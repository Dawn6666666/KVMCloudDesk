# 后续开发 Prompt

你正在接手 `D:\Code\Other\kvm` 项目，仓库已推送到 `https://github.com/Dawn6666666/KVMCloudDesk.git`。请先阅读 `RPD.md`、`环境信息.md`、`README.md`、`docs/architecture.md`、`docs/client-web-plan.md`、`docs/deploy-centos.md`。

## 当前项目状态

项目为 Maven 多模块：

```text
kvm-cloud-manager
├── common
├── backend
├── client-swing
└── docs
```

已完成：

- `common` DTO、Request、`ApiResponse`。
- `backend` Spring Boot REST API。
- `mock` profile 完整可用。
- `libvirt` profile 已通过 JNA 连接 CentOS `/usr/lib64/libvirt.so.0` 和 `qemu:///system`。
- `client-swing` 已有可运行基础版。
- CentOS 可通过 `ssh centos` 连接。

CentOS 环境要点：

```text
IP: 192.168.61.130
libvirt URI: qemu:///system
libvirt library: /usr/lib64/libvirt.so.0
已有 VM: demo
default 网络: 存在
default 存储池: 存在
镜像目录: /var/lib/libvirt/images
```

已实测通过的 libvirt API：

- 宿主机信息
- 虚拟机列表/详情
- 启动、关机、强制关闭、暂停、恢复
- 网络列表、default 网络停止/启动
- 存储池列表、存储卷列表
- 镜像目录扫描
- 快照列表

已知限制：

- `demo` 使用 raw 磁盘，libvirt 不支持 raw 内部快照，所以创建快照会返回真实错误。
- `shutdown` 依赖 guest 响应 ACPI，demo 可能不会立刻关机，`destroy` 可强制关闭。
- 业务代码禁止调用 `virsh`、`Runtime.exec`、`ProcessBuilder` 来实现功能。

## 新任务目标

不要删除 `client-swing`。在现有项目上新增 `client-web`，使用以下技术栈实现 Web 前端，并复用现有 Spring Boot API：

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts

## 开发要求

1. 新增 `client-web` 目录，不改动后端业务接口，除非确实需要 CORS 或配置支持。
2. 开发期优先使用 Vite proxy 转发 `/api` 到 `http://127.0.0.1:8080`。
3. 封装 Axios：
   - 统一处理 `ApiResponse<T>`。
   - `success=false` 抛业务错误。
   - 网络错误用 Element Plus 提示。
4. 建立 TypeScript 类型，对应 common DTO。
5. 使用 Pinia 存储：
   - 后端地址/连接状态。
   - 操作日志。
6. 使用 Vue Router 建页面：
   - Dashboard
   - Host
   - VMs
   - Images
   - Networks
   - Snapshots
   - Storage
7. Dashboard 用 ECharts：
   - 内存使用率
   - VM 状态统计
   - 存储池容量
8. 所有危险操作需要确认弹窗。
9. 所有异步按钮需要 loading 状态。
10. 不要在前端直接访问 libvirt，不要调用命令行。

## 推荐实现顺序

1. 初始化 `client-web` Vite Vue TypeScript 项目。
2. 安装 Element Plus、Pinia、Vue Router、Axios、ECharts。
3. 配置 Vite proxy。
4. 实现 `src/types/kvm.ts`。
5. 实现 `src/api/http.ts` 和 `src/api/kvm.ts`。
6. 实现 `MainLayout.vue`。
7. 实现 Dashboard、Host、VM 页面。
8. 实现 Images、Networks、Snapshots、Storage 页面。
9. 启动 mock 后端测试 Web 前端。
10. 通过 `ssh centos` 部署/连接 libvirt 后端测试真实数据。
11. 更新 README 和 docs。
12. `npm run build`、`mvn clean package -DskipTests` 验证。
13. 提交并推送，提交信息使用中文且详细。

## 常用命令

Windows 本地 JDK 21：

```powershell
$env:JAVA_HOME='C:\Users\24831\.jdks\ms-21.0.10'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
```

后端 mock：

```bash
java -jar backend/target/kvm-cloud-backend.jar --spring.profiles.active=mock
```

CentOS libvirt 后端测试：

```bash
scp backend/target/kvm-cloud-backend.jar centos:/tmp/kvm-cloud-backend.jar
ssh centos "cd /tmp; nohup java -jar /tmp/kvm-cloud-backend.jar --spring.profiles.active=libvirt --server.address=127.0.0.1 --server.port=18080 > /tmp/kvm-cloud-backend.log 2>&1 < /dev/null &"
ssh centos "curl -s http://127.0.0.1:18080/api/vms"
```

检查业务代码禁用项：

```bash
rg "Runtime\.getRuntime|ProcessBuilder|virsh" backend client-swing common client-web
```
