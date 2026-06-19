# Windows 运行说明

要求：

- JDK 21
- Maven

构建：

```bash
mvn clean package
```

启动 mock 后端：

```bash
java -jar backend/target/kvm-cloud-backend.jar --spring.profiles.active=mock
```

启动 Swing 客户端：

```bash
java -jar client-swing/target/kvm-cloud-client.jar
```

Windows 阶段不要启动 `libvirt` profile，不需要安装 libvirt。
