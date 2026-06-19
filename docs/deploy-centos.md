# CentOS 部署步骤

部署目标：

- CentOS Stream 10
- Java 21
- libvirt URI：`qemu:///system`
- libvirt 动态库：`/usr/lib64/libvirt.so.0`
- 后端监听：`0.0.0.0:8080`

开放端口：

```bash
firewall-cmd --add-port=8080/tcp --permanent
firewall-cmd --reload
```

启动后端：

```bash
java -jar kvm-cloud-backend.jar \
  --spring.profiles.active=libvirt \
  --server.address=0.0.0.0 \
  --server.port=8080
```

Windows 客户端连接地址：

```text
http://192.168.61.130:8080
```

## 当前 libvirt 支持状态

已在 CentOS 上验证：

- `GET /api/host/info`
- `GET /api/vms`
- `GET /api/vms/demo`
- `POST /api/vms/demo/start`
- `POST /api/vms/demo/suspend`
- `POST /api/vms/demo/resume`
- `POST /api/vms/demo/shutdown`
- `POST /api/vms/demo/destroy`
- `GET /api/networks`
- `POST /api/networks/default/stop`
- `POST /api/networks/default/start`
- `GET /api/storage/pools`
- `GET /api/storage/pools/default/volumes`
- `GET /api/vms/demo/snapshots`

`shutdown` 依赖 guest 响应正常关机信号，测试中 demo 虚拟机可能不会立刻关机；`destroy` 可强制恢复到关闭状态。

`demo` 当前磁盘为 raw 格式，libvirt 返回“raw 不支持内部快照”，因此 `POST /api/vms/demo/snapshots` 在该环境中会失败并返回明确错误。后续可使用 qcow2 虚拟机验证快照创建、恢复和删除。
