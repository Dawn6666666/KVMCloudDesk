# 后续维护 Prompt

你正在接手 `d:/Code/Other/kvm` 项目。在开始工作之前，请先阅读 [RPD.md](file:///d:/Code/Other/kvm/RPD.md)、[环境信息.md](file:///d:/Code/Other/kvm/环境信息.md)、[README.md](file:///d:/Code/Other/kvm/README.md) 以及 [deploy-centos.md](file:///d:/Code/Other/kvm/docs/deploy-centos.md)。

## 当前项目状态

项目为多模块结构，各模块职责如下：

- `common`：提供 DTO、请求对象以及统一响应结构。
- `backend`：提供 REST API，支持 mock 和 libvirt 双运行模式。在 libvirt 模式下，已实现通过 JNA 连接系统底层动态库 `/usr/lib64/libvirt.so.0` 和 `qemu:///system`。
- `client-swing`：基于 Swing 编写的桌面客户端。
- `client-web`：基于 Vue 3 和 TypeScript 编写的网页客户端，采用深色玻璃拟态设计，使用 ECharts 渲染系统资源监控图表，并集成全局操作日志追踪面板。

CentOS 真实部署环境配置：

- 主机 IP：`192.168.61.130`
- libvirt 连接路径：`qemu:///system`
- libvirt 动态链接库：`/usr/lib64/libvirt.so.0`
- 默认虚拟网络：`default`
- 默认存储池：`default`（路径为 `/var/lib/libvirt/images`）
- 测试用虚拟机：`demo`（使用 raw 磁盘），以及为了验证快照和虚拟机创建而部署的 `cirros-test-052`（使用 qcow2 磁盘）与 `tinycore-test`。

已在 CentOS 真实环境中实测通过的接口与功能：

- 宿主机系统信息：已优化获取主机名逻辑，移除了导致 15 秒反向解析超时的原生 virConnectGetHostname 调用，改为秒级读取。
- 虚拟机列表及详情查询。
- 虚拟机创建：支持在 libvirt 下依据所选的系统镜像或光盘进行创建并自动定义。
- 虚拟机删除：支持在 libvirt 下取消虚拟机定义，并物理删除关联的磁盘映像文件。
- 虚拟机控制操作：包括启动、关机、强制关闭、暂停、恢复。
- 虚拟网络查询与开关：包括启动和停止默认网络。
- 存储池与存储卷的列表查询。
- 快照全生命周期管理：包括快照创建、列表查询、恢复回滚、彻底删除（已通过 qcow2 虚机实测通过）。

已知开发约束：

- 在 Windows 本地开发时，不要启动 libvirt 配置，只能运行 mock 配置。
- 虚拟机控制中的关机动作依赖虚拟机内部的系统响应，若需要立即断电请使用强制关闭。
- 业务代码中严禁通过 Runtime.getRuntime().exec()、ProcessBuilder 或其他任何方式调用 virsh 命令行。

## 常用命令与联调指南

### 后端 mock 运行模式

```bash
java -jar backend/target/kvm-cloud-backend.jar --spring.profiles.active=mock
```

### CentOS 真实 libvirt 后端部署

1. 编译后端：
   ```bash
   mvn clean package -DskipTests
   ```
2. 将 jar 包复制并部署到 CentOS 服务器上，以 libvirt 模式启动：
   ```bash
   java -jar kvm-cloud-backend.jar --spring.profiles.active=libvirt --server.address=0.0.0.0 --server.port=8080
   ```

### 网页客户端开发与远程联调

1. 进入网页客户端目录并启动调试服务器：
   ```bash
   cd client-web
   npm install
   npm run dev
   ```
2. 本地调试默认代理至本地 8080 端口。若需直接联调 CentOS 上的真实后端，可修改 [vite.config.ts](file:///d:/Code/Other/kvm/client-web/vite.config.ts) 中的代理配置：
   ```ts
   server: {
     proxy: {
       '/api': {
         target: 'http://192.168.61.130:8080',
         changeOrigin: true
       }
     }
   }
   ```
   修改后本地网页端将直接通过代理调用远程 CentOS 后端，实现真实硬件环境下的前端交互调试。

### 检查代码禁用项

在代码修改后，可通过以下命令检查是否无意中引入了被禁用的 virsh 命令行调用：

```bash
rg "Runtime\.getRuntime|ProcessBuilder|virsh" backend client-swing common client-web
```
