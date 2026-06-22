# 系统架构说明

## 总体架构

系统采用 C/S 架构。后端提供 REST API，前端支持 Swing 桌面客户端与 Vue 3 Web 客户端。客户端通过 HTTP 交互，不直接依赖 libvirt。

### 开发阶段：Windows mock 模式

```text
Windows
  Web 客户端与 Swing 客户端
      ↓ HTTP JSON
  Spring Boot 后端 mock 运行模式
      ↓
  ConcurrentHashMap 内存数据
```

### 部署阶段：CentOS libvirt 模式

```text
Windows 浏览器与客户端
      ↓ HTTP JSON
  CentOS Spring Boot 后端 libvirt 运行模式
      ↓ JNA
  /usr/lib64/libvirt.so.0
      ↓
  qemu:///system
      ↓
  KVM / QEMU
```

### 网页直连控制台数据流

```text
Windows 浏览器 noVNC
      ↓  WebSocket 协议
  CentOS Spring Boot 后端 VncProxyWebSocketHandler
      ↓  TCP 套接字
  CentOS 虚拟机真实的 VNC 物理端口
```

## Profile 隔离

`mock` 与 `libvirt` 通过 Spring `@Profile` 注解隔离。libvirt 相关组件如 `LibvirtConnectionManager` 与各类 Libvirt 服务只在 `@Profile("libvirt")` 下注册，确保 Windows 模拟启动时不会加载 Linux 动态库。

后端配置通过 `application-{profile}.yml` 切换：

| 配置项 | mock | libvirt |
| :--- | :--- | :--- |
| `kvm.libvirt.uri` | `mock:///system` | `qemu:///system` |
| `kvm.libvirt.library` | `none` | `/usr/lib64/libvirt.so.0` |
| `kvm.image.dir` | `./mock-images` | `/var/lib/libvirt/images` |

## 关键设计模式

### 模板方法 — withDomain / withNetwork / withPool

所有 libvirt 域操作通过 `withDomain` 回调完成连接管理和资源释放：

```java
private <T> T withDomain(String name, DomainCallback<T> callback) {
    Pointer conn = manager.open();
    Pointer domain = lib.virDomainLookupByName(conn, name);
    try {
        return callback.apply(domain);
    } finally {
        lib.virDomainFree(domain);
        manager.close(conn);
    }
}
```

类似模式用于 `withNetwork`、`withPool`、`withSnapshot`。注意 `LibvirtConnectionManager.open()` 每次新建连接，没有连接池。

### 统一异常处理

`BusinessException` 由 `GlobalExceptionHandler` 捕获，返回 HTTP 400 + `ApiResponse.fail(message)`。未知异常返回 HTTP 500。

### XML 安全

`LibvirtUtil.xml()` 解析 libvirt 返回的 XML 时启用 XXE 防护：禁用 DOCTYPE 声明、关闭实体引用展开。

### 统一响应封装

所有接口返回 `ApiResponse<T>`，前端通过 Axios 拦截器统一解包 `data` 字段，失败时弹出 `ElMessage.error` 并写入操作日志。

## JNA 映射

`LibvirtLibrary` 接口通过 JNA 映射 libvirt C API，共约 57 个函数和 4 个 Structure：

| 类别 | 函数数 | 主要功能 |
| :--- | :--- | :--- |
| 连接与主机 | 10 | 连接管理、主机信息、版本检测 |
| 域 | 15 | 生命周期、状态查询、XML 获取、创建定义 |
| 网络 | 10 | 网络列表、启停、XML 获取 |
| 快照 | 8 | 快照创建、查询、更新与删除 |
| 存储 | 14 | 存储池/卷列表、信息查询 |

4 个 JNA 结构体：表示主机硬件的 `VirNodeInfo`、表示虚拟机状态的 `VirDomainInfo`、表示存储池的 `VirStoragePoolInfo` 以及表示存储卷的 `VirStorageVolInfo`。

## 客户端

### client-swing

Java Swing 桌面客户端，1180×760 窗口，使用 FlatLaf 支持明暗主题切换，采用 MigLayout 布局、JSplitPane 导航与 CardLayout 页面切换。所有 HTTP 调用通过基于 SwingWorker 封装的 `SwingTasks.run()` 在后台线程执行，结果在 EDT 上进行回调。

### client-web

Vue 3 + TypeScript Web 客户端。暖色大地色调设计，主色为 `#ca6a1f`，背景为 `#f8f6f2`，使用 Fira Sans 与 Fira Code 字体。主要特性：

- **VNC 网页直连控制台**：在虚拟机实例处于运行状态下，支持一键拉起独立的控制台路由页面，借由 noVNC 将前端操作映射为二进制事件流，通过后端代理通道直接对虚拟主机的命令行或图形桌面进行完全的键鼠操控。
- **静态资源 Fallback 重定向**：为解决单页应用的 History 路由在直接输入 URL 或刷新页面时导致后端 404 错误的问题，在后端的 WebConfig 中重定向了所有属于前端的路由路径，包括 `/vnc/**` 通配路径，统一将其 forward 转发至 `/index.html`，完美维持了浏览器的状态不丢失。
- **物理性能与网络吞吐监控**：仪表盘支持展示宿主机物理负载及网络吞吐实时走势折线图，提供高精度网络瞬时速率展示，且历史指标采样周期扩展至 30 个点。
- **多磁盘与重启操作**：支持虚拟机实例一键重启操作并与操作日志对接，同时支持虚拟机多磁盘 XML 递归配置解析，可正确计算并显示挂载的所有磁盘映像路径及累计大小。
- **网络子网掩码与关联虚拟机**：支持在网络管理页面解析显示子网掩码参数，且由前端自动动态统计并展示各虚拟局域网络下挂载的虚拟机总数。
- **镜像实际占用与完好性校验**：区分基础系统镜像的最大分配容量与物理占用空间，并提供物理路径完好性状态指示灯。
- **快照活跃分支高亮**：自动识别虚拟机当前活跃的快照分支并高亮标记，且翻译快照生成时的虚拟机状态。
- **Vite 代理**：开发时通过 Vite 代理转发 `/api` 请求到后端。

两套客户端均通过统一的 HTTP API 与后端 Spring Boot 交互，不直接依赖 libvirt 动态链接库或执行任何 virsh 命令行程序。
