# 报告素材

## 模块划分

- `common`：共享 DTO、请求对象与统一响应对象 `ApiResponse<T>`。
- `backend`：REST API、Spring Profile 双模式切换、mock/libvirt 两套服务实现。
- `client-swing`：Swing 桌面 GUI、HTTP API 封装、操作日志展示。
- `client-web`：Vue 3 Web 界面，复用后端 REST 接口，提供 Dashboard 图表和操作日志追踪。

## 核心类 — 后端

| 类 | 说明 |
| :--- | :--- |
| `BackendApplication` | Spring Boot 入口 |
| `GlobalExceptionHandler` | 统一异常处理，将 BusinessException 映射为 400，其他异常映射为 500 |
| `BusinessException` | 业务异常基类 |
| `MockDataStore` | 模拟模式内存数据仓库，基于 ConcurrentHashMap 实现 |
| `MockVmService` | 模拟模式虚拟机服务，包含状态机逻辑 |
| `LibvirtLibrary` | JNA 接口定义，映射约 57 个函数与 4 个结构体 |
| `LibvirtConnectionManager` | libvirt 连接管理，处理动态库加载、连接建立与释放以及内存释放 |
| `LibvirtUtil` | 工具方法，包含返回值校验、XXE 安全 XML 解析、版本解码及容量转换 |
| `LibvirtVmService` | libvirt 模式虚拟机服务，提供 withDomain 回调、domain XML 生成与状态映射功能 |
| `LibvirtHostService` | libvirt 模式宿主机服务，通过分析 /proc/meminfo、主机名解析链进行版本检测 |
| `LibvirtNetworkService` | libvirt 模式网络服务 |
| `LibvirtSnapshotService` | libvirt 模式快照服务 |
| `LibvirtStorageService` | libvirt 模式存储服务 |
| `LibvirtResourceFallbackServices` | libvirt 模式镜像服务（扫描 /var/lib/libvirt/images） |

## 核心类 — Swing 客户端

| 类 | 说明 |
| :--- | :--- |
| `ClientApplication` | Swing 入口，包含 FlatLaf 主题初始化 |
| `ClientConfig` | 配置加载，自 client.properties 与命令行参数读取 |
| `BackendApiClient` | HTTP API 封装，使用 Java HttpClient 与 Jackson |
| `SwingTasks` | SwingWorker 异步工具，用于后台执行与 EDT 回调 |
| `MainFrame` | 主窗口，提供 JSplitPane 导航、CardLayout 页面切换与明暗主题切换 |
| `PanelSupport` | 面板基类，定义工具栏、表格、对话框与日志的通用接口 |
| `HostPanel` | 宿主机信息卡片面板 |
| `VmPanel` | 虚拟机管理面板，包含 11 列表格、8 操作按钮与创建对话框 |
| `ImagePanel` | 镜像管理面板 |
| `NetworkPanel` | 网络管理面板 |
| `SnapshotPanel` | 快照管理面板，包含虚拟机选择器与快照表格 |
| `StoragePanel` | 存储管理面板，包含存储池与存储卷的上下分栏布局 |

## 核心文件 — Web 客户端

| 文件 | 说明 |
| :--- | :--- |
| `main.ts` | Vue 入口，处理 Element Plus、Pinia 与 Router 的注册 |
| `router/index.ts` | 路由配置，包含 7 个页面的路由映射 |
| `api/http.ts` | Axios 封装，提供请求与响应拦截器以及操作日志处理 |
| `api/kvm.ts` | 包含 22 个 API 请求函数，支持携带操作描述请求头 |
| `stores/backendStore.ts` | 宿主机连接状态 Pinia Store |
| `stores/logStore.ts` | 操作日志 Pinia 状态库，最多存储 200 条日志 |
| `layouts/MainLayout.vue` | 页面主布局，提供侧边栏导航、顶部状态与底部日志面板 |
| `views/DashboardView.vue` | 控制面板视图，包含 4 个统计卡片与 3 个 ECharts 图表 |
| `views/VmView.vue` | 虚拟机管理页面，包含状态联动按钮、创建表单与关机轮询进度对话框 |
| `views/ImageView.vue` | 镜像管理 |
| `views/NetworkView.vue` | 网络管理 |
| `views/SnapshotView.vue` | 快照管理页面，包含恢复时的警告提示 |
| `views/StorageView.vue` | 存储管理页面，包含存储池与存储卷的主从布局及容量进度条 |
| `views/HostView.vue` | 宿主机详情 |
| `types/kvm.ts` | TypeScript 类型定义，与 Java 端的 DTO 结构对应 |
| `style.css` | 全局样式表，定义了主题色、CSS 变量与 Element Plus 组件样式覆盖 |

## 核心类 — common 模块

| 类 | 说明 |
| :--- | :--- |
| `ApiResponse<T>` | 统一响应封装结构 |
| `HostInfoDto` | 宿主机信息传输对象，包含 13 个属性字段 |
| `VmInfoDto` | 虚拟机信息传输对象，包含 12 个属性字段 |
| `ImageInfoDto` | 镜像信息 |
| `NetworkInfoDto` | 网络信息 |
| `SnapshotInfoDto` | 快照信息 |
| `StoragePoolInfoDto` | 存储池信息 |
| `StorageVolumeInfoDto` | 存储卷信息 |
| `CreateVmRequest` | 创建虚拟机请求 |
| `AddImageRequest` | 添加镜像请求 |
| `CreateSnapshotRequest` | 创建快照请求 |

## 双模式说明

`mock` 配置使用内存数据完成 Windows 调试。`libvirt` 配置通过 JNA 加载 `/usr/lib64/libvirt.so.0` 并连接 `qemu:///system`。

当前真实 libvirt 已支持宿主机信息、虚拟机列表、虚拟机详情、创建虚拟机、删除虚拟机、启动、关机、强制关闭、暂停、恢复、网络列表、网络启停、存储池列表、存储卷列表、快照列表以及快照创建、恢复与删除接口。镜像列表在 libvirt 模式下扫描 `/var/lib/libvirt/images`。

快照测试说明：使用 raw 格式磁盘的虚拟机，受底层存储驱动限制不支持快照功能，调用相关接口时返回格式不支持的错误。目前已在 CentOS 环境中部署 qcow2 格式磁盘的虚拟机，验证了快照创建、查询、恢复与删除操作。

## 运行截图位置

建议保存到：

```text
docs/screenshots/
```
