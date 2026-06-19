# 系统架构说明

第一阶段架构：

```text
Windows
  Swing 客户端
    -> HTTP JSON
  Spring Boot 后端 mock profile
    -> 内存假数据
```

后续 CentOS 架构：

```text
Windows Swing 客户端
  -> HTTP JSON
CentOS Spring Boot 后端 libvirt profile
  -> JNA
  -> /usr/lib64/libvirt.so.0
  -> qemu:///system
```

`mock` 与 `libvirt` 通过 Spring Profile 隔离。libvirt 相关 Bean 只在 `@Profile("libvirt")` 下注册，确保 Windows mock 启动不会加载 Linux 动态库。
