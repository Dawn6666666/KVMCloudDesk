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

当前已实现 `client-swing`。后续计划新增 `client-web`，使用 Vue 3 + Vite + TypeScript + Element Plus 实现浏览器端管理界面。

`client-web` 不新增后端业务协议，不直接调用 libvirt，不调用命令行，只通过 Axios 访问已有 `/api` REST 接口。Swing 客户端保留，Web 客户端作为并行展示端。
