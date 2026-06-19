# KVM 云平台管理系统项目需求文档

## 1. 项目名称

`kvm-cloud-manager`

中文名称：KVM 云平台管理系统
课程设计题目：云平台管理系统
课程背景：《KVM 虚拟化实践与编程》课程设计

---

## 2. 项目目标

开发一套基于 KVM + libvirt 的图形化云平台管理系统，用于模拟中小型私有云平台的虚拟化资源管理场景。

系统需要支持：

1. 宿主机信息查看；
2. 镜像资源管理；
3. 虚拟机生命周期管理；
4. 网络管理；
5. 快照管理；
6. 存储池、存储卷查询与管理；
7. Windows 开发阶段使用 mock 数据完整调试；
8. CentOS 部署阶段切换为真实 libvirt 模式管理 KVM 虚拟机。

---

## 3. 总体架构

项目采用客户端 / 服务端架构。

### 3.1 Windows 开发阶段

```text
Windows IDEA
    ↓
Java Swing 客户端
    ↓ HTTP JSON
Java Spring Boot 后端 mock 模式
    ↓
内存假数据
```

开发阶段目标：

1. 在 Windows 上完整开发 GUI；
2. 在 Windows 上运行后端 mock 模式；
3. Swing 客户端通过 HTTP API 调用 mock 后端；
4. 完整测试页面、按钮、表格刷新、状态变化、错误提示、日志输出；
5. 不在 Windows 上加载 libvirt.so；
6. 不要求 Windows 直接连接 qemu:///system。

### 3.2 CentOS 部署阶段

```text
Windows
    ↓
Java Swing 客户端
    ↓ HTTP JSON
CentOS
    ↓
Java Spring Boot 后端 libvirt 模式
    ↓
JNA 调用 /usr/lib64/libvirt.so.0
    ↓
qemu:///system
    ↓
KVM / QEMU / libvirt
```

部署阶段目标：

1. 后端部署到 CentOS；
2. 使用 `libvirt` profile；
3. 通过 JNA 调用系统 libvirt 动态库；
4. 连接 `qemu:///system`；
5. 管理 CentOS 中真实 KVM 虚拟机；
6. Windows Swing 客户端只需要修改后端地址即可连接真实后端。

---

## 4. 当前已知运行环境

### 4.1 Windows 宿主机

用途：

1. 开发完整项目；
2. 运行 IntelliJ IDEA；
3. 运行 Swing 客户端；
4. 运行 mock 后端；
5. 通过 HTTP 访问 CentOS 后端。

要求：

1. JDK 21；
2. Maven；
3. IntelliJ IDEA；
4. Windows 本地不需要安装 libvirt；
5. Windows 本地不需要有 `/usr/lib64/libvirt.so.0`；
6. Windows 本地不需要连接 `qemu:///system`。

### 4.2 CentOS 虚拟机

用途：

1. 运行 Java 后端 libvirt 模式；
2. 调用真实 libvirt API；
3. 管理 KVM 虚拟机。

已知环境：

```text
系统：CentOS Stream 10
JDK：OpenJDK 21
虚拟化平台：KVM + QEMU + libvirt
libvirt URI：qemu:///system
libvirt 动态库：/usr/lib64/libvirt.so.0
网卡 IP：192.168.61.130
default NAT 网络：存在
default 存储池：存在
已有虚拟机：demo
镜像目录：/var/lib/libvirt/images
```

CentOS 后端默认监听：

```text
0.0.0.0:8080
```

Windows 客户端访问地址：

```text
http://192.168.61.130:8080
```

---

## 5. 技术栈要求

### 5.1 后端技术栈

必须使用：

```text
Java 21
Spring Boot
Spring Web / Spring MVC
Spring Profile
Maven
JNA
Jackson
```

后端职责：

1. 提供 REST API；
2. 根据 profile 切换 mock 模式和 libvirt 模式；
3. mock 模式使用内存数据；
4. libvirt 模式调用真实 libvirt；
5. 统一返回 JSON；
6. 统一异常处理；
7. 不提供 GUI；
8. 不调用 virsh 命令实现业务功能。

### 5.2 客户端技术栈

必须使用：

```text
Java 21
Swing
FlatLaf
MigLayout
Java HttpClient
Jackson
Maven
```

客户端职责：

1. 提供图形化管理界面；
2. 通过 HTTP API 访问后端；
3. 展示宿主机、虚拟机、镜像、网络、快照、存储等数据；
4. 提供按钮操作虚拟机生命周期；
5. 提供日志区；
6. 不直接调用 libvirt；
7. 不直接连接 CentOS；
8. 不调用 virsh。

---

## 6. 强制开发限制

### 6.1 严禁事项

项目业务代码中禁止出现以下行为：

```java
Runtime.getRuntime().exec("virsh ...");
new ProcessBuilder("virsh", "...").start();
```

禁止通过命令行方式实现：

1. 虚拟机列表；
2. 虚拟机启动；
3. 虚拟机关机；
4. 虚拟机强制关闭；
5. 虚拟机暂停；
6. 虚拟机恢复；
7. 虚拟机删除；
8. 快照创建；
9. 快照恢复；
10. 网络管理；
11. 存储管理。

### 6.2 允许事项

`virsh` 只能用于人工调试环境，不能写进项目业务代码。

允许在 README 或报告中写：

```text
手动验证命令：
virsh list --all
virsh net-list --all
virsh pool-list --all
```

但不允许程序自动执行这些命令。

---

## 7. Maven 多模块结构

请创建 Maven 多模块项目：

```text
kvm-cloud-manager
├── pom.xml
├── common
│   ├── pom.xml
│   └── src/main/java
│       └── com/example/kvm/common
│           ├── dto
│           ├── request
│           └── response
├── backend
│   ├── pom.xml
│   └── src/main/java
│       └── com/example/kvm/backend
│           ├── BackendApplication.java
│           ├── controller
│           ├── service
│           ├── service/mock
│           ├── service/libvirt
│           ├── config
│           ├── exception
│           └── util
└── client-swing
    ├── pom.xml
    └── src/main/java
        └── com/example/kvm/client
            ├── ClientApplication.java
            ├── api
            ├── config
            ├── ui
            ├── ui/component
            └── util
```

推荐包名：

```text
com.example.kvm
```

也可以使用：

```text
com.nyist.kvm
```

---

## 8. common 模块要求

`common` 模块存放 DTO、请求对象、通用响应对象。

### 8.1 ApiResponse

```java
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
```

所有后端接口统一返回：

```json
{
  "success": true,
  "message": "操作成功",
  "data": {}
}
```

### 8.2 HostInfoDto

字段：

```text
hostname
cpuModel
cpuCount
cpuMHz
totalMemoryMb
usedMemoryMb
freeMemoryMb
memoryUsagePercent
virtualizationType
libvirtVersion
qemuVersion
kvmEnabled
connectionUri
```

### 8.3 VmInfoDto

字段：

```text
name
uuid
state
cpuCount
memoryMb
diskPath
diskSizeGb
networkName
ipAddress
autostart
persistent
description
```

状态建议统一为中文展示：

```text
运行
关闭
暂停
异常
未知
```

### 8.4 CreateVmRequest

字段：

```text
name
cpuCount
memoryMb
diskSizeGb
imageName
networkName
description
```

### 8.5 ImageInfoDto

字段：

```text
name
path
format
sizeGb
createTime
description
```

### 8.6 AddImageRequest

字段：

```text
name
path
description
```

### 8.7 NetworkInfoDto

字段：

```text
name
uuid
active
autostart
bridgeName
forwardMode
ipAddress
dhcpStart
dhcpEnd
```

### 8.8 SnapshotInfoDto

字段：

```text
name
vmName
createTime
state
description
```

### 8.9 CreateSnapshotRequest

字段：

```text
name
description
```

### 8.10 StoragePoolInfoDto

字段：

```text
name
uuid
active
autostart
path
capacityGb
availableGb
allocationGb
```

### 8.11 StorageVolumeInfoDto

字段：

```text
name
path
type
capacityGb
allocationGb
```

---

## 9. backend 模块要求

后端使用 Spring Boot。

### 9.1 后端 profile

必须支持两个 profile：

```text
mock
libvirt
```

#### mock profile

用途：

1. Windows 开发；
2. 本地测试；
3. 不依赖 libvirt；
4. 不加载 JNA；
5. 不加载 `/usr/lib64/libvirt.so.0`；
6. 使用内存数据模拟虚拟化资源。

启动方式：

```bash
java -jar backend.jar --spring.profiles.active=mock
```

#### libvirt profile

用途：

1. CentOS 部署；
2. 调用真实 libvirt；
3. 连接 `qemu:///system`；
4. 使用 JNA 加载 `/usr/lib64/libvirt.so.0`。

启动方式：

```bash
java -jar backend.jar --spring.profiles.active=libvirt
```

### 9.2 application 配置

`backend/src/main/resources/application.yml`

```yaml
server:
  port: 8080

spring:
  application:
    name: kvm-cloud-backend

kvm:
  libvirt:
    uri: qemu:///system
    library: /usr/lib64/libvirt.so.0
  image:
    dir: /var/lib/libvirt/images
```

`application-mock.yml`

```yaml
spring:
  config:
    activate:
      on-profile: mock

kvm:
  mode: mock
```

`application-libvirt.yml`

```yaml
spring:
  config:
    activate:
      on-profile: libvirt

kvm:
  mode: libvirt
  libvirt:
    uri: qemu:///system
    library: /usr/lib64/libvirt.so.0
  image:
    dir: /var/lib/libvirt/images
```

### 9.3 后端 Service 接口

必须定义以下接口：

```text
HostService
VmService
ImageService
NetworkService
SnapshotService
StorageService
```

### 9.4 HostService

方法：

```java
HostInfoDto getHostInfo();
```

mock 实现：

```text
返回固定宿主机假数据。
```

libvirt 实现：

```text
通过 libvirt API 获取宿主机信息。
```

### 9.5 VmService

方法：

```java
List<VmInfoDto> listVms();

VmInfoDto getVm(String name);

VmInfoDto createVm(CreateVmRequest request);

void startVm(String name);

void shutdownVm(String name);

void destroyVm(String name);

void suspendVm(String name);

void resumeVm(String name);

void deleteVm(String name);
```

mock 实现：

```text
用 ConcurrentHashMap<String, VmInfoDto> 存储虚拟机。
操作按钮改变虚拟机 state。
```

libvirt 实现：

```text
通过 JNA 调用 libvirt 实现真实操作。
```

### 9.6 ImageService

方法：

```java
List<ImageInfoDto> listImages();

ImageInfoDto addImage(AddImageRequest request);

void deleteImage(String name);
```

mock 实现：

```text
用 List / Map 维护镜像假数据。
```

libvirt 实现：

```text
第一阶段可以先扫描 /var/lib/libvirt/images 下的 img/qcow2/iso 文件。
后续可接入 libvirt storage volume API。
```

### 9.7 NetworkService

方法：

```java
List<NetworkInfoDto> listNetworks();

void startNetwork(String name);

void stopNetwork(String name);
```

mock 实现：

```text
维护 default 网络假数据。
```

libvirt 实现：

```text
通过 libvirt API 查询网络。
```

### 9.8 SnapshotService

方法：

```java
List<SnapshotInfoDto> listSnapshots(String vmName);

SnapshotInfoDto createSnapshot(String vmName, CreateSnapshotRequest request);

void revertSnapshot(String vmName, String snapshotName);

void deleteSnapshot(String vmName, String snapshotName);
```

mock 实现：

```text
用 Map<String, List<SnapshotInfoDto>> 维护每台虚拟机的快照。
```

libvirt 实现：

```text
通过 libvirt domain snapshot API 实现。
```

### 9.9 StorageService

方法：

```java
List<StoragePoolInfoDto> listPools();

List<StorageVolumeInfoDto> listVolumes(String poolName);
```

mock 实现：

```text
返回 default 存储池和若干假存储卷。
```

libvirt 实现：

```text
通过 libvirt storage pool / volume API 查询。
```

---

## 10. 后端 REST API

所有接口统一前缀：

```text
/api
```

### 10.1 宿主机接口

```text
GET /api/host/info
```

返回：

```text
ApiResponse<HostInfoDto>
```

### 10.2 虚拟机接口

```text
GET    /api/vms
GET    /api/vms/{name}
POST   /api/vms
POST   /api/vms/{name}/start
POST   /api/vms/{name}/shutdown
POST   /api/vms/{name}/destroy
POST   /api/vms/{name}/suspend
POST   /api/vms/{name}/resume
DELETE /api/vms/{name}
```

### 10.3 镜像接口

```text
GET    /api/images
POST   /api/images
DELETE /api/images/{name}
```

### 10.4 网络接口

```text
GET  /api/networks
POST /api/networks/{name}/start
POST /api/networks/{name}/stop
```

### 10.5 快照接口

```text
GET    /api/vms/{vmName}/snapshots
POST   /api/vms/{vmName}/snapshots
POST   /api/vms/{vmName}/snapshots/{snapshotName}/revert
DELETE /api/vms/{vmName}/snapshots/{snapshotName}
```

### 10.6 存储接口

```text
GET /api/storage/pools
GET /api/storage/pools/{poolName}/volumes
```

---

## 11. mock 模式详细要求

mock 模式必须先完整实现，保证 Windows 上能跑通整个系统。

### 11.1 宿主机假数据

```text
hostname: centos
cpuModel: AMD Ryzen 5 3600 6-Core Processor
cpuCount: 4
cpuMHz: 3593
totalMemoryMb: 7647
usedMemoryMb: 1700
freeMemoryMb: 5947
memoryUsagePercent: 22
virtualizationType: KVM
libvirtVersion: 11.10.0
qemuVersion: 10.1.0
kvmEnabled: true
connectionUri: qemu:///system
```

### 11.2 虚拟机假数据

虚拟机 1：

```text
name: demo
uuid: mock-demo-uuid
state: 关闭
cpuCount: 2
memoryMb: 1024
diskPath: /var/lib/libvirt/images/ubuntu16.04.7.img
diskSizeGb: 10
networkName: default
ipAddress: -
autostart: false
persistent: true
description: 演示虚拟机
```

虚拟机 2：

```text
name: test-centos
uuid: mock-test-centos-uuid
state: 运行
cpuCount: 1
memoryMb: 512
diskPath: /var/lib/libvirt/images/test-centos.qcow2
diskSizeGb: 5
networkName: default
ipAddress: 192.168.122.101
autostart: false
persistent: true
description: 测试虚拟机
```

### 11.3 镜像假数据

```text
ubuntu16.04.7.img
cirros.qcow2
centos-test.qcow2
```

每条镜像数据需要包含：

```text
名称
路径
格式
大小
创建时间
描述
```

### 11.4 网络假数据

```text
name: default
uuid: mock-default-network-uuid
active: true
autostart: true
bridgeName: virbr0
forwardMode: nat
ipAddress: 192.168.122.1
dhcpStart: 192.168.122.2
dhcpEnd: 192.168.122.254
```

### 11.5 快照假数据

demo 虚拟机：

```text
snapshot-001
snapshot-002
```

test-centos 虚拟机：

```text
base-snapshot
```

### 11.6 存储假数据

存储池：

```text
name: default
active: true
autostart: true
path: /var/lib/libvirt/images
capacityGb: 47
availableGb: 18
allocationGb: 29
```

存储卷：

```text
ubuntu16.04.7.img
test-centos.qcow2
cirros.qcow2
```

### 11.7 mock 行为

虚拟机操作：

```text
startVm:
  如果状态是“关闭”，改为“运行”
  如果已经运行，返回成功但提示“虚拟机已在运行”

shutdownVm:
  如果状态是“运行”或“暂停”，改为“关闭”

destroyVm:
  强制改为“关闭”

suspendVm:
  如果状态是“运行”，改为“暂停”
  如果不是运行，返回错误提示

resumeVm:
  如果状态是“暂停”，改为“运行”
  如果不是暂停，返回错误提示

deleteVm:
  从 Map 中删除虚拟机

createVm:
  新增虚拟机记录
  初始状态为“关闭”
```

快照操作：

```text
createSnapshot:
  为指定虚拟机新增快照记录

revertSnapshot:
  返回成功，日志提示已恢复

deleteSnapshot:
  删除指定快照
```

网络操作：

```text
startNetwork:
  active 改为 true

stopNetwork:
  active 改为 false
```

镜像操作：

```text
addImage:
  新增镜像记录

deleteImage:
  删除镜像记录
```

---

## 12. libvirt 模式要求

libvirt 模式部署在 CentOS 上。

### 12.1 libvirt 加载要求

只允许在 `libvirt` profile 下加载 libvirt 动态库。

示例结构：

```java
@Service
@Profile("libvirt")
public class LibvirtConnectionManager {
    // 只在 libvirt profile 下初始化 JNA
}
```

禁止在 mock profile 下执行：

```java
Native.load("virt", LibvirtLibrary.class);
```

### 12.2 JNA 动态库路径

CentOS 上 libvirt 动态库路径：

```text
/usr/lib64/libvirt.so.0
```

配置项：

```yaml
kvm:
  libvirt:
    library: /usr/lib64/libvirt.so.0
    uri: qemu:///system
```

### 12.3 最小 JNA 函数映射

第一阶段至少映射：

```text
virConnectOpen
virConnectClose
virConnectGetHostname
virConnectGetLibVersion
virConnectListAllDomains
virDomainGetName
virDomainGetUUIDString
virDomainGetInfo
virDomainGetState
virDomainCreate
virDomainShutdown
virDomainDestroy
virDomainSuspend
virDomainResume
virDomainUndefine
virDomainFree
```

第二阶段映射：

```text
virConnectListAllNetworks
virNetworkGetName
virNetworkIsActive
virNetworkCreate
virNetworkDestroy
virNetworkFree
```

第三阶段映射：

```text
virDomainSnapshotCreateXML
virDomainSnapshotLookupByName
virDomainRevertToSnapshot
virDomainSnapshotDelete
virDomainSnapshotFree
```

第四阶段映射：

```text
virConnectListAllStoragePools
virStoragePoolGetName
virStoragePoolGetInfo
virStoragePoolListAllVolumes
virStorageVolGetName
virStorageVolGetPath
virStorageVolGetInfo
```

### 12.4 libvirt 模式实现优先级

第一优先级必须真实实现：

```text
连接 qemu:///system
获取宿主机信息
获取虚拟机列表
获取虚拟机状态
启动虚拟机
关闭虚拟机
强制关闭虚拟机
暂停虚拟机
恢复虚拟机
```

第二优先级：

```text
网络列表
存储池列表
快照列表
创建快照
恢复快照
删除快照
```

第三优先级：

```text
镜像管理
创建虚拟机
删除虚拟机
存储卷管理
```

如果 JNA 真实调用在短时间内难以全部完成，必须保证：

```text
mock 模式完整可用
libvirt 模式至少能列出真实虚拟机并执行启动、关闭、暂停、恢复
```

---

## 13. client-swing 模块要求

客户端为 Java Swing 桌面程序，运行在 Windows 上。

### 13.1 客户端配置

客户端需要支持配置后端地址。

配置文件：

```text
client.properties
```

开发阶段：

```properties
backend.url=http://127.0.0.1:8080
```

连接 CentOS：

```properties
backend.url=http://192.168.61.130:8080
```

也可以支持启动参数：

```bash
java -jar client-swing.jar --backend.url=http://192.168.61.130:8080
```

### 13.2 客户端 API 封装

创建：

```text
BackendApiClient
```

职责：

1. 封装所有 HTTP 请求；
2. 使用 Java 21 HttpClient；
3. 使用 Jackson 解析 JSON；
4. 不在 UI 类里直接写 HTTP 细节；
5. 所有方法返回 DTO 或抛出业务异常。

需要实现方法：

```java
HostInfoDto getHostInfo();

List<VmInfoDto> listVms();

VmInfoDto getVm(String name);

VmInfoDto createVm(CreateVmRequest request);

void startVm(String name);

void shutdownVm(String name);

void destroyVm(String name);

void suspendVm(String name);

void resumeVm(String name);

void deleteVm(String name);

List<ImageInfoDto> listImages();

ImageInfoDto addImage(AddImageRequest request);

void deleteImage(String name);

List<NetworkInfoDto> listNetworks();

void startNetwork(String name);

void stopNetwork(String name);

List<SnapshotInfoDto> listSnapshots(String vmName);

SnapshotInfoDto createSnapshot(String vmName, CreateSnapshotRequest request);

void revertSnapshot(String vmName, String snapshotName);

void deleteSnapshot(String vmName, String snapshotName);

List<StoragePoolInfoDto> listPools();

List<StorageVolumeInfoDto> listVolumes(String poolName);
```

### 13.3 Swing 主题

必须引入 FlatLaf。

默认主题：

```text
FlatLightLaf
```

可选支持：

```text
FlatDarculaLaf
```

启动时设置：

```java
FlatLightLaf.setup();
```

### 13.4 Swing 布局

推荐使用：

```text
MigLayout
BorderLayout
CardLayout
JSplitPane
JTabbedPane
JTable
JProgressBar
JTextArea
JOptionPane
```

避免复杂动画，避免 WebView，避免 JavaFX。

### 13.5 主界面设计

主窗口：

```text
MainFrame
```

布局：

```text
┌─────────────────────────────────────────────────────┐
│ KVM 云平台管理系统        后端: 127.0.0.1:8080 已连接 │
├───────────────┬─────────────────────────────────────┤
│ 宿主机概览     │                                     │
│ 虚拟机管理     │                                     │
│ 镜像管理       │              当前功能页              │
│ 网络管理       │                                     │
│ 快照管理       │                                     │
│ 存储管理       │                                     │
├───────────────┴─────────────────────────────────────┤
│ 日志：系统启动成功                                    │
└─────────────────────────────────────────────────────┘
```

左侧导航：

```text
宿主机概览
虚拟机管理
镜像管理
网络管理
快照管理
存储管理
```

顶部状态栏：

```text
系统名称
后端地址
连接状态
当前时间
刷新按钮
主题切换按钮
```

底部日志区：

```text
显示用户操作结果、错误信息、后端返回信息。
```

---

## 14. 客户端页面详细要求

### 14.1 HostPanel 宿主机概览

展示字段：

```text
主机名
CPU 型号
CPU 核心数
CPU 主频
总内存
已用内存
内存使用率
虚拟化类型
libvirt 版本
QEMU 版本
KVM 是否可用
连接 URI
```

控件要求：

```text
卡片式展示
JProgressBar 显示内存使用率
刷新按钮
```

### 14.2 VmPanel 虚拟机管理

表格列：

```text
名称
状态
CPU
内存
磁盘路径
磁盘大小
网络
IP 地址
自动启动
持久化
描述
```

按钮：

```text
刷新
创建
启动
关机
强制关闭
暂停
恢复
删除
```

操作要求：

1. 必须先选择虚拟机才能操作；
2. 操作前弹出确认框；
3. 操作后刷新表格；
4. 操作结果写入日志；
5. 操作失败用 JOptionPane 提示。

创建虚拟机对话框字段：

```text
虚拟机名称
CPU 核数
内存大小
磁盘大小
选择镜像
选择网络
描述
```

### 14.3 ImagePanel 镜像管理

表格列：

```text
名称
格式
大小
路径
创建时间
描述
```

按钮：

```text
刷新
添加镜像
删除镜像
```

添加镜像对话框字段：

```text
镜像名称
镜像路径
描述
```

### 14.4 NetworkPanel 网络管理

表格列：

```text
名称
状态
自动启动
网桥名称
转发模式
IP 地址
DHCP 起始地址
DHCP 结束地址
```

按钮：

```text
刷新
启动网络
停止网络
```

### 14.5 SnapshotPanel 快照管理

布局：

```text
上方：虚拟机选择下拉框
中间：快照表格
下方：操作按钮
```

表格列：

```text
快照名称
虚拟机名称
创建时间
状态
描述
```

按钮：

```text
刷新
创建快照
恢复快照
删除快照
```

### 14.6 StoragePanel 存储管理

存储池表格列：

```text
名称
状态
自动启动
路径
总容量
已分配
可用容量
```

存储卷表格列：

```text
名称
路径
类型
总容量
已分配容量
```

按钮：

```text
刷新存储池
查看存储卷
```

---

## 15. Swing 线程要求

所有 HTTP 请求必须放在后台线程。

禁止在事件分发线程 EDT 中直接请求后端。

推荐使用：

```java
SwingWorker
```

按钮操作示例逻辑：

```text
点击按钮
    ↓
禁用按钮
    ↓
SwingWorker.doInBackground 调用 BackendApiClient
    ↓
SwingWorker.done 更新 JTable / JOptionPane / 日志
    ↓
恢复按钮
```

---

## 16. 错误处理要求

### 16.1 后端错误处理

后端需要统一异常处理：

```text
GlobalExceptionHandler
```

所有错误返回：

```json
{
  "success": false,
  "message": "错误原因",
  "data": null
}
```

### 16.2 客户端错误处理

客户端遇到错误时：

1. 弹窗提示；
2. 写入日志；
3. 不崩溃；
4. 不关闭主窗口。

常见错误：

```text
无法连接后端
后端返回失败
虚拟机不存在
当前状态不允许操作
JSON 解析失败
网络超时
```

---

## 17. 日志要求

### 17.1 后端日志

后端使用 Spring Boot 默认日志即可。

需要记录：

```text
接口访问
虚拟机操作
libvirt 连接失败
libvirt 操作失败
mock 操作
```

### 17.2 客户端日志

客户端底部日志区显示：

```text
[18:30:01] 系统启动
[18:30:03] 已连接后端：http://127.0.0.1:8080
[18:31:10] 启动虚拟机 demo 成功
[18:31:25] 暂停虚拟机 demo 成功
```

---

## 18. UI 美化要求

客户端界面需要比默认 Swing 更现代。

必须使用：

```text
FlatLaf
MigLayout
```

UI 风格：

```text
浅色主题
左侧导航
卡片式信息展示
表格化资源管理
按钮分组
底部日志区
顶部连接状态
```

按钮风格建议：

```text
主操作：启动、创建、刷新
危险操作：删除、强制关闭
普通操作：暂停、恢复、关机
```

可以使用简单图标或 Unicode 符号，但不要依赖复杂图标库。

---

## 19. 开发顺序

请严格按以下顺序开发。

### 第 1 阶段：项目骨架

1. 创建 Maven 父项目；
2. 创建 `common` 模块；
3. 创建 `backend` 模块；
4. 创建 `client-swing` 模块；
5. 确保所有模块能编译。

验收标准：

```bash
mvn clean package
```

能通过。

### 第 2 阶段：common DTO

1. 实现所有 DTO；
2. 实现 ApiResponse；
3. 实现请求对象；
4. 确保后端和客户端都能引用 common 模块。

验收标准：

```text
common 模块无编译错误。
```

### 第 3 阶段：后端 mock 模式

1. 实现 Controller；
2. 实现 Service 接口；
3. 实现 MockService；
4. 支持 `mock` profile；
5. 提供所有 REST API。

验收标准：

启动：

```bash
java -jar backend.jar --spring.profiles.active=mock
```

测试：

```bash
curl http://127.0.0.1:8080/api/host/info
curl http://127.0.0.1:8080/api/vms
```

能返回 JSON。

### 第 4 阶段：Swing 客户端 API

1. 实现 BackendApiClient；
2. 实现配置读取；
3. 能调用后端接口；
4. 能处理 ApiResponse。

验收标准：

```text
客户端可以成功调用 mock 后端并打印虚拟机列表。
```

### 第 5 阶段：Swing 主界面

1. 实现 MainFrame；
2. 设置 FlatLaf；
3. 实现左侧导航；
4. 实现底部日志；
5. 实现顶部状态栏。

验收标准：

```text
Windows 上可打开主窗口。
```

### 第 6 阶段：功能页面

按顺序实现：

```text
HostPanel
VmPanel
ImagePanel
NetworkPanel
SnapshotPanel
StoragePanel
```

验收标准：

```text
Windows 上通过 Swing 客户端完整操作 mock 后端。
```

### 第 7 阶段：CentOS mock 部署

1. 打包后端；
2. 上传到 CentOS；
3. 在 CentOS 上运行 mock profile；
4. Windows 客户端连接 CentOS mock 后端。

验收标准：

```text
Windows Swing 客户端访问 http://192.168.61.130:8080 并正常显示 mock 数据。
```

### 第 8 阶段：libvirt 最小真实实现

先实现：

```text
连接 qemu:///system
宿主机信息
虚拟机列表
启动虚拟机
关机虚拟机
强制关闭虚拟机
暂停虚拟机
恢复虚拟机
```

验收标准：

```text
CentOS libvirt profile 后端能管理 demo 虚拟机。
```

### 第 9 阶段：完善 libvirt 功能

再实现：

```text
网络列表
存储池列表
快照列表
创建快照
恢复快照
删除快照
镜像列表
创建虚拟机
删除虚拟机
```

---

## 20. 打包要求

### 20.1 后端打包

生成：

```text
backend/target/kvm-cloud-backend.jar
```

启动 mock：

```bash
java -jar kvm-cloud-backend.jar --spring.profiles.active=mock
```

启动 libvirt：

```bash
java -jar kvm-cloud-backend.jar --spring.profiles.active=libvirt
```

### 20.2 客户端打包

生成：

```text
client-swing/target/kvm-cloud-client.jar
```

启动：

```bash
java -jar kvm-cloud-client.jar
```

客户端需要支持读取：

```text
client.properties
```

---

## 21. README 要求

请生成 README.md，内容包括：

```text
项目简介
系统架构
技术栈
模块说明
Windows 开发运行方式
CentOS 部署方式
mock profile 使用方式
libvirt profile 使用方式
API 列表
禁止 virsh 说明
常见问题
```

---

## 22. 报告素材要求

为了方便后续写课程设计报告，项目中请保留以下素材：

```text
系统架构说明
模块划分说明
核心类说明
REST API 列表
mock 与 libvirt 双模式说明
运行截图位置
部署步骤
```

建议创建：

```text
docs/
├── architecture.md
├── api.md
├── deploy-centos.md
├── run-windows.md
└── report-notes.md
```

---

## 23. 最小验收功能 MVP

必须优先完成这些功能：

```text
1. 后端 mock 模式可启动；
2. Swing 客户端可连接 mock 后端；
3. 宿主机信息可展示；
4. 虚拟机列表可展示；
5. 启动虚拟机按钮可用；
6. 关机虚拟机按钮可用；
7. 强制关闭按钮可用；
8. 暂停按钮可用；
9. 恢复按钮可用；
10. 删除按钮可用；
11. 镜像列表可展示；
12. 网络列表可展示；
13. 快照列表可展示；
14. 创建快照、恢复快照、删除快照在 mock 模式可演示；
15. CentOS libvirt 模式至少能列出真实虚拟机并操作 demo 虚拟机。
```

---

## 24. 编程 agent 执行要求

请按以下方式开发：

1. 先输出项目实现计划；
2. 再创建项目结构；
3. 每完成一个阶段，说明已完成内容；
4. 优先保证 mock 全链路跑通；
5. 不要一开始就卡在 libvirt JNA；
6. 真实 libvirt 服务可以先写骨架；
7. 所有代码保持可编译；
8. 不要在业务代码中调用 virsh；
9. 不要让客户端依赖 libvirt；
10. 不要让 mock profile 加载 libvirt.so；
11. UI 要使用 FlatLaf 美化；
12. 所有耗时操作使用 SwingWorker；
13. 后端所有接口返回 ApiResponse；
14. 关键代码需要添加必要注释；
15. 项目最终必须能在 Windows IDEA 中运行 mock 模式。

---

## 25. 最终交付物

最终项目需要包含：

```text
1. Maven 多模块源码；
2. common 模块；
3. backend 模块；
4. client-swing 模块；
5. mock profile；
6. libvirt profile；
7. Swing 图形化客户端；
8. README.md；
9. docs 文档；
10. 可打包 jar；
11. Windows 开发运行说明；
12. CentOS 部署运行说明。
```

---

## 26. 一句话总结

本项目采用 Java Swing + Spring Boot 的客户端/服务端架构。Windows 阶段通过 mock profile 完整开发和测试图形化管理系统；CentOS 阶段通过 libvirt profile 调用真实 libvirt API 管理 KVM 虚拟机。客户端只调用 HTTP API，后端通过 profile 决定使用 mock 数据还是真实 libvirt，项目代码中严禁调用 virsh 命令实现业务功能。
