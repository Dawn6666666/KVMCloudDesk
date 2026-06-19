# 系统架构说明

第一阶段架构：

```text
Windows
  Swing 客户端 / Web 客户端
    -> HTTP JSON
  Spring Boot 后端 mock profile
    -> 内存假数据
```

后续 CentOS 架构：

```text
Windows Swing 客户端 / Web 客户端
  -> HTTP JSON
CentOS Spring Boot 后端 libvirt profile
  -> JNA
  -> /usr/lib64/libvirt.so.0
  -> qemu:///system
```

`mock` 与 `libvirt` 通过 Spring Profile 隔离。libvirt 相关 Bean 只在 `@Profile("libvirt")` 下注册，确保 Windows mock 启动不会加载 Linux 动态库。

## 客户端演进

目前系统已完整实现 `client-swing` 和 `client-web` 双重客户端支持。

- `client-swing`：主要面向桌面原生应用程序要求，使用 Swing 开发。
- `client-web`：作为并行的高级客户端，基于 Vue 3 + Vite + TypeScript 构建，通过 Axios 访问已有的 `/api` 接口。Web 端采用深色毛玻璃及渐变设计系统，具备更好的图表呈现与运行日志追溯体验。

两套客户端均通过统一的 HTTP API 与后端 Spring Boot 交互，不直接依赖 libvirt 动态链接库或执行任何 virsh 命令行程序。
