# 系统架构说明

## 总体架构

系统采用 C/S 架构。后端提供 REST API，前端支持 Swing 桌面客户端与 Vue 3 Web 客户端。客户端通过 HTTP 交互，不直接依赖 libvirt。

### 开发阶段（Windows mock 模式）

```text
Windows
  Web 客户端 / Swing 客户端
      ↓ HTTP JSON
  Spring Boot 后端 mock profile
      ↓
  ConcurrentHashMap 内存数据
```

### 部署阶段（CentOS libvirt 模式）

```text
Windows 浏览器或客户端
      ↓ HTTP JSON
CentOS Spring Boot 后端 libvirt profile
      ↓ JNA
/usr/lib64/libvirt.so.0
      ↓
qemu:///system
      ↓
KVM / QEMU
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

Java Swing 桌面客户端，1180×760 窗口，使用 FlatLaf 支持明暗主题切换（包含 FlatLightLaf 与 FlatDarculaLaf），采用 MigLayout 布局、JSplitPane 导航与 CardLayout 页面切换。所有 HTTP 调用通过基于 SwingWorker 封装的 `SwingTasks.run()` 在后台线程执行，结果在 EDT 上进行回调。

### client-web

Vue 3 + TypeScript Web 客户端。暖色大地色调设计（主色 `#ca6a1f`，背景 `#f8f6f2`），使用 Fira Sans / Fira Code 字体。主要特性：

- **Dashboard**：包含 4 个统计卡片与 3 个 ECharts 图表，分别展示内存使用率环形图、虚拟机状态饼图与存储池容量堆叠条形图。
- **操作日志**：API 层通过 `X-Action-Description` 自定义请求头传递操作描述，通过拦截器自动记录至 Pinia 状态库，并在底部日志面板显示。
- **关机轮询**：关机后每 1.5 秒轮询虚拟机状态，最多 15 次（共计约 22.5 秒），进度条采用渐近递增方式，超时后显示强制断电按钮。
- **Vite 代理**：开发时通过 Vite 代理转发 `/api` 请求到后端。

两套客户端均通过统一的 HTTP API 与后端 Spring Boot 交互，不直接依赖 libvirt 动态链接库或执行任何 virsh 命令行程序。
