# client-web 开发规划

## 目标

在现有项目上新增 `client-web` 模块，使用现代 Web 技术栈实现 KVM 云平台管理界面。Web 前端复用现有 Spring Boot REST API，不改写后端业务架构，不直接依赖 libvirt。

## 技术栈

- Vue 3
- Vite
- TypeScript
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts

## 推荐目录

```text
client-web
├── package.json
├── index.html
├── vite.config.ts
├── tsconfig.json
└── src
    ├── main.ts
    ├── App.vue
    ├── router
    │   └── index.ts
    ├── api
    │   ├── http.ts
    │   └── kvm.ts
    ├── stores
    │   ├── backendStore.ts
    │   └── logStore.ts
    ├── layouts
    │   └── MainLayout.vue
    ├── views
    │   ├── DashboardView.vue
    │   ├── HostView.vue
    │   ├── VmView.vue
    │   ├── ImageView.vue
    │   ├── NetworkView.vue
    │   ├── SnapshotView.vue
    │   └── StorageView.vue
    └── types
        └── kvm.ts
```

## API 复用原则

后端统一返回：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

Axios 封装应统一处理：

- `success=false` 时抛业务错误。
- 网络错误用 Element Plus 消息提示。
- 操作成功/失败写入 Pinia 日志 store。
- 不在 Vue 页面中散落 HTTP 细节。

## 开发期代理

Vite 开发服务器建议配置：

```ts
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://127.0.0.1:8080',
      changeOrigin: true
    }
  }
}
```

连接 CentOS 后端时可将 target 改为：

```text
http://192.168.61.130:8080
```

## 页面优先级

1. 主布局：左侧菜单、顶部后端状态、底部/侧边日志。
2. Dashboard：宿主机概览、内存使用率、VM 状态统计、存储容量图表。
3. 虚拟机管理：列表、启动、关机、强制关闭、暂停、恢复、删除确认。
4. 镜像管理：列表、添加、删除。
5. 网络管理：列表、启动、停止。
6. 快照管理：选择 VM、列表、创建、恢复、删除。
7. 存储管理：存储池、存储卷。

## UI 建议

- 使用 Element Plus 的 `el-container`、`el-menu`、`el-table`、`el-dialog`、`el-form`、`el-tag`、`el-button`、`el-message`。
- Dashboard 使用 ECharts 展示内存使用率、VM 状态分布、存储池容量。
- 危险操作使用二次确认。
- 所有异步按钮操作要有 loading 状态。
- 表格刷新后保留清晰日志。

## 与 Swing 的关系

`client-swing` 保留，满足原课程文档中的 Swing 客户端要求。`client-web` 是增强版客户端，用于更现代的展示和后续扩展。两个客户端都只访问 Spring Boot REST API。
