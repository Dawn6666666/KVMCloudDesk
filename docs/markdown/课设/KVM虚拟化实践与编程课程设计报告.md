南 阳 理 工 学 院

《KVM虚拟化实践与编程课程设计》报告

# 云平台管理系统

|      |      |      |
|------|------|------|
| 姓名 | 学号 | 成绩 |
|      |      |      |

> 专业： 软件工程
>
> 班级： 24云计算1班
>
> 指导教师： 单平平

南阳理工学院计算机与软件学院
2026年06月

---

# 1 虚拟化环境的搭建

## 1.1 课题研究的背景与意义
随着企业信息化进程的加速，私有云和混合云平台在中小型企业及高校教学实验室中的应用越来越广泛。虚拟化作为云平台的最核心支撑技术，其管理效率、响应速度和稳定性直接关系到云平台的运营质量。KVM与libvirt作为开源社区中最为主流的虚拟化引擎和管理套件，提供了强大的虚拟化底层支撑能力。然而，传统的 libvirt 命令行管理工具（如 virsh）对于普通的系统管理员而言存在较高的学习与操作门槛，不仅命令繁多、参数复杂，且在网络配置和状态实时展示上缺乏直观性。因此，设计并开发一套可视化的云平台管理系统，将物理主机的硬件指标实时监控、多台虚拟机的生命周期管控、快照以及存储卷进行集中可视管理，对于降低私有云运维难度、提升虚拟化资源利用效率具有非常重要的实用价值。

## 1.2 CentOS Stream 10 虚拟化环境搭建步骤
本系统运行的真实物理服务器采用 CentOS Stream 10 操作系统，嵌套部署于 VMware 虚拟机环境。具体虚拟化环境搭建步骤如下：

### 1.2.1 物理虚拟化支持检测与内核模块加载
在 CentOS 服务器上执行以下命令以确认 CPU 是否支持硬件虚拟化（AMD-V）及内核 KVM 模块是否正确加载：
```bash
# 1. 检查物理 CPU 虚拟化指令集（包含 svm 标志代表支持 AMD-V，vmx 代表 Intel VT-x）
grep -E 'vmx|svm' /proc/cpuinfo

# 2. 检测内核 KVM 模块加载状态，确保 kvm 与 kvm_amd 均已装载
lsmod | grep kvm

# 3. 确认 /dev/kvm 设备全局可读写权限
ls -la /dev/kvm

# 4. 确认嵌套虚拟化（Nested Virtualization）已开启，此配置对于虚拟机中运行虚拟机至关重要
cat /sys/module/kvm_amd/parameters/nested
```
经检测，CPU 核包含 svm 标志，内核 kvm_amd 模块已成功加载，且嵌套虚拟化参数返回 `1`，表示嵌套虚拟化已顺利开启。

### 1.2.2 模块化虚拟化服务安装与启动
CentOS Stream 10 抛弃了传统的单一守护进程 `libvirtd`，改用模块化的守护进程架构。在此架构中，针对 QEMU 的服务为 `virtqemud`：
```bash
# 1. 启动并启用 QEMU 守护进程 socket 激活机制
systemctl enable virtqemud.socket --now
systemctl start virtqemud

# 2. 启动虚拟锁管理辅助服务，避免多实例并发修改虚拟机磁盘镜像导致冲突
systemctl enable virtlockd --now
systemctl start virtlockd

# 3. 验证 libvirt 服务连接状态
virsh -c qemu:///system list --all
```
在模块化架构下，直接执行 `systemctl status libvirtd` 显示 `inactive` 是正常现象，`virtqemud` 会在接收到 client 端请求时自动拉起。

### 1.2.3 开放防火墙端口
为了支持本地开发端或网页浏览器直接远程调用 CentOS 宿主机上的 API 接口，需要放行后端服务所占用的 8080 端口，并放行 VNC 虚拟桌面所需的 5900-5910 端口：
```bash
# 1. 放行 8080 网关端口与 5901 VNC 端口
firewall-cmd --add-port=8080/tcp --permanent
firewall-cmd --add-port=5901/tcp --permanent
firewall-cmd --reload
```

## 1.3 关键系统配置优化（故障解决）
在部署联调过程中，我们针对 CentOS 宿主机环境进行了两个至关重要的底层系统级调优，彻底排除了两项隐蔽的底层运行故障：

### 1.3.1 解决主机名反向解析导致的 15 秒启动延迟
在真实硬件环境实测中发现，执行 `virsh start` 启动虚拟机时会卡顿整整 15 秒才返回成功，同时前端接口也会出现 `timeout of 15000ms exceeded` 超时报错。
*   **故障定位**：经分析，系统主机名为 `centos`，但在本地的 `/etc/hosts` 中并未配置主机名的本地回环映射。当 libvirt 在创建虚机网口、网桥或建立连接时，会调用系统函数查询宿主机名，由于本地未命中，会转为向外部 DNS 发送查询，直到 15 秒超时等待后才 fallback 返回，引起严重的阻塞。
*   **解决方案**：在 CentOS 虚拟机的 `/etc/hosts` 文件末尾追加本机的回环解析映射，使系统查询立即在本地命中：
    ```text
    127.0.0.1 centos
    ::1 centos
    ```
*   **优化结果**：修复后，主机名解析时间由 **15.108秒** 降至 **0.002秒**，`virsh start` 虚拟机启动延迟由 **15.169秒** 降至 **0.148秒**，网页端响应彻底恢复为毫秒级。

### 1.3.2 排除废弃模拟器程序干扰，修复 QEMU 版本检测
在请求 `GET /api/host/info` 获取系统参数时，接口返回的 `qemuVersion` 总是为 `"未知"`。
*   **故障定位**：检查 `/tmp/kvm.log` 后端日志，注意到 libvirt 会抛出错误：`不支持的配置：QEMU 版本 >= 6.2.0 是必需的，但找到了 4 1.0`。经排查，CentOS 宿主机中除了系统自带的 `/usr/libexec/qemu-kvm` (10.1.0) 外，在 `/usr/local/bin/` 目录下还残留了一个老旧的独立 QEMU 4.1.0 运行程序。libvirt 在探测系统能力时由于扫描到了该低版本模拟器且判定不合规，导致能力探测机制发生级联崩溃，进而使 `virConnectGetVersion` API 直接返回失败。
*   **解决方案**：重命名屏蔽该废弃程序，并重启守护服务使 libvirt 聚焦于系统默认的主体模拟器：
    ```bash
    mv /usr/local/bin/qemu-system-x86_64 /usr/local/bin/qemu-system-x86_64.bak
    systemctl restart virtqemud
    ```
*   **优化结果**：`virsh version` 获取 hypervisor 版本成功输出为 `QEMU 10.1.0`，后端接口成功正确展示 `QEMU 模拟器版本: 10.1.0`。

![图1-1 虚拟化环境搭建与优化完成截图](报告模板v5_media/media/image_placeholder.png)
*(图1-1 虚拟化环境搭建与优化完成截图)*

---

# 2 需求分析

## 2.1 系统整体需求与划分
本系统面向私有云网络管理员，要求系统全功能替代命令行控制台。核心需求具体细分为以下五个模块：
1.  **宿主机资源看板**：获取并图形化展示宿主机硬件规格（内存总量、剩余内存、CPU规格主频、核心数等）及虚拟化组件版本号（libvirt版本、QEMU版本等）。
2.  **镜像生命周期管理**：支持从光盘（ISO格式）或磁盘映像（qcow2/raw格式）添加已部署的系统模版，作为虚拟机定义的镜像源，且支持对已添加的模板执行物理删除。
3.  **虚拟机统一生命周期管控**：支持虚拟机新建配置（名称、核心数、内存空间、磁盘配额、镜像绑定、局域网段绑定等），支持虚拟机实例的启动、暂停、恢复、关机（ACPI指令下发）、断电（物理强制关闭）及定义注销（并清除关联的物理磁盘）。
4.  **虚拟网络调配**：管理 libvirt 网桥（`default`），实时查询子网段及网桥分配状态，提供启停 NAT 网络的功能。
5.  **无损快照管理**：支持对运行中或关闭中的虚机（qcow2磁盘格式）进行瞬间快照拍摄、指定快照版本回滚，以及冗余快照的删除。

## 2.2 核心业务用例规约设计

为清晰化描述系统管理员的核心操作逻辑，下表针对“正常安全关机”这一高复杂度业务设计了详细的用例规约。

### 表2-1 正常安全关机（Shutdown）用例规约表

| 规约项目 | 详细描述内容 |
| :--- | :--- |
| **用例名称** | 安全关闭虚拟机实例（Shutdown VM） |
| **用例参与者** | 系统管理员 |
| **前置条件** | 目标虚拟机实例在系统列表中处于“运行”或“暂停”状态 |
| **基本事件流** | 1. 管理员在前端列表中定位正在运行的虚机，点击“关机”操作；<br>2. 系统弹出“安全关机控制”进度条弹窗，状态显示 `正在等待虚拟机关闭`；<br>3. 后端向虚拟机下发 ACPI 关机指令信号；<br>4. 前端启动 1.5 秒频率的状态轮询，监测虚机实时状态，进度条平滑递增；<br>5. 虚机内部系统响应该关机信号，安全保存数据并正常关闭电源；<br>6. 轮询检测到虚机状态已转为“关闭”，进度条置为 100% 成功状态；<br>7. 系统在 1.2 秒后自动隐藏弹窗，写入 `虚拟机已成功安全关机` 审计日志，并刷新列表。 |
| **异常事件流** | **异常 1a. 虚拟机不支持 ACPI 或内部关机守护进程（如 acpid）未运行**：<br>&nbsp;&nbsp;&nbsp;&nbsp;1. 发送关机信号后，虚拟机状态在 22.5 秒内始终未能转为“关闭”；<br>&nbsp;&nbsp;&nbsp;&nbsp;2. 轮询达到 15 次上限，前端判定响应超时，进度条变红进入 Exception 异常状态；<br>&nbsp;&nbsp;&nbsp;&nbsp;3. 弹窗中吐出引导文本并显示出 `强制断电` 红色按钮，写入 `虚拟机关机响应超时` 错误日志；<br>&nbsp;&nbsp;&nbsp;&nbsp;4. 管理员可点击 `强制断电` 下发物理销毁电源指令，虚机强行关闭。 |
| **后置条件** | 虚拟机状态更新为“关闭”，释放占用的宿主机 CPU 与内存运行资源 |

## 2.3 系统用例图

### 2.3.1 系统管理员整体用例图
![图2-1 系统管理员整体用例图](报告模板v5_media/media/image_placeholder.png)
*(图2-1 系统管理员整体用例图)*

### 2.3.2 虚拟机生命周期管理子用例图
![图2-2 系统管理员虚拟机生命周期管理用例图](报告模板v5_media/media/image_placeholder.png)
*(图2-2 系统管理员虚拟机生命周期管理用例图)*

---

# 3 系统设计

## 3.1 系统物理与逻辑架构设计
系统采用前后端完全分离的 C/S 结构：
*   **Windows 开发 mock 数据环境**：为了让开发人员在 Windows 宿主机（无 KVM 和 libvirt C 库）上能够直接调试前端的复杂网络与页面，设计了 MockProfile，使用基于 ConcurrentHashMap 构建的内存模拟器替代真实的 libvirt 接口，实现完整的业务链路模拟，确保 Windows 本地启动正常，不发生加载 Linux `.so` 的故障。
*   **CentOS 真实部署环境**：激活 LibvirtProfile，后端引入 JNA，映射 `/usr/lib64/libvirt.so.0` 的原生 C 语言接口，通过句柄连接 `qemu:///system` 以 root 权限对本地的真实虚拟机进行零命令行原生控制。

```mermaid
graph TD
    A[网页客户端 Vue 3 / TS] -->|HTTP REST API| B[Spring Boot 后端网关]
    B -->|@Profile mock| C[内存假数据 MockService]
    B -->|@Profile libvirt / JNA| D[libvirt C 动态库]
    D -->|qemu:///system| E[KVM/QEMU 虚机内核]
```

## 3.2 数据结构设计 (DTO)
前后端通过强类型的 JSON 对象交互。以下是 common 共享模块中设计的两个最核心的资源传输对象：

### 表3-1 HostInfoDto 宿主机信息传输对象字段表

| 字段名称 (Field) | 数据类型 (Type) | 映射含义与作用说明 |
| :--- | :--- | :--- |
| `hostname` | `String` | 宿主机操作系统主机名 |
| `cpuModel` | `String` | 物理 CPU 架构与型号（如 x86_64 / EPYC） |
| `cpuCount` | `int` | 物理 CPU 线程/核心数 |
| `cpuMHz` | `int` | 物理 CPU 运行主频 |
| `totalMemoryMb` | `long` | 物理服务器的内存总量 (MB) |
| `usedMemoryMb` | `long` | 宿主机已使用的内存容量 (MB) |
| `freeMemoryMb` | `long` | 宿主机当前空闲可支配内存量 (MB) |
| `memoryUsagePercent` | `int` | 内存占用百分比 (0-100) |
| `virtualizationType`| `String` | 虚拟化技术类型（KVM） |
| `libvirtVersion` | `String` | 服务器端宿主 libvirt API 的版本号 |
| `qemuVersion` | `String` | 物理机运行的 QEMU 模拟器版本 |
| `connectionUri` | `String` | 后端驱动连接路径句柄（qemu:///system） |

### 表3-2 VmInfoDto 虚拟机详情传输对象字段表

| 字段名称 (Field) | 数据类型 (Type) | 映射含义与作用说明 |
| :--- | :--- | :--- |
| `name` | `String` | 虚拟机实例唯一名称 |
| `uuid` | `String` | 虚拟机唯一识别 UUID (36位字符) |
| `state` | `String` | 虚机当前状态（映射为中文：运行/关闭/暂停/异常） |
| `cpuCount` | `int` | 虚拟机分配的虚拟 CPU 核心数 |
| `memoryMb` | `int` | 虚拟机分配的物理内存大小 (MB) |
| `diskPath` | `String` | 虚拟机磁盘在宿主机存储池中的物理路径 |
| `diskSizeGb` | `int` | 虚拟机磁盘的最大可用配额 (GB) |
| `networkName` | `String` | 虚拟机网卡桥接的虚拟网络名称（如 default） |
| `autostart` | `boolean` | 是否配置为宿主机开机自动启动虚拟机 |
| `persistent` | `boolean` | 虚拟机配置是否已持久化定义在系统 XML 中 |
| `description` | `String` | 虚拟机的备注说明信息 |

---

# 4 系统实现

## 4.1 后端 libvirt 生命周期管控的核心实现
后端 `LibvirtVmService` 通过 JNA 原生调用 libvirt 动态链接库的 `virDomainCreate`、`virDomainShutdown` 等方法实现虚拟机的开机与关机，其关键代码如下所示：

```java
// 虚拟机生命周期控制实现
@Service
@Profile("libvirt")
public class LibvirtVmService implements VmService {
    private final LibvirtConnectionManager manager;

    public LibvirtVmService(LibvirtConnectionManager manager) {
        this.manager = manager;
    }

    // 启动虚拟机实现
    @Override
    public void startVm(String name) {
        withDomain(name, domain -> {
            check(manager.library().virDomainCreate(domain), "启动虚拟机失败：" + name);
            return null;
        });
    }

    // 发送 ACPI 关机信号
    @Override
    public void shutdownVm(String name) {
        withDomain(name, domain -> {
            check(manager.library().virDomainShutdown(domain), "发送关机信号失败：" + name);
            return null;
        });
    }

    // 物理强制关闭（断电）
    @Override
    public void destroyVm(String name) {
        withDomain(name, domain -> {
            check(manager.library().virDomainDestroy(domain), "强制关闭虚拟机失败：" + name);
            return null;
        });
    }

    // 虚拟机操作通用句柄包装，用于安全地打开连接、解析域并注销 C 指针内存防止泄漏
    private <T> T withDomain(String name, DomainCallback<T> callback) {
        LibvirtLibrary lib = manager.library();
        Pointer conn = manager.open();
        Pointer domain = lib.virDomainLookupByName(conn, name);
        if (domain == null) {
            manager.close(conn);
            throw new BusinessException("虚拟机不存在：" + name);
        }
        try {
            return callback.apply(domain);
        } finally {
            lib.virDomainFree(domain); // 释放虚机句柄指针，规避 JNA 内存泄漏
            manager.close(conn);
        }
    }
}
```

## 4.2 前端安全关机进度轮询与定时器回收实现
前端在 VmView.vue 中，针对 ACPI 信号无响应和异常退出的场景实现了防抖和优雅的销毁回收逻辑。在弹窗关闭事件中注销全部 Interval 定时器，防范后台请求累积：

```typescript
let activePollInterval: any = null;
let activeProgressInterval: any = null;

// 关闭窗口事件：彻底注销并回收定时器，防范后台请求累积
const handleShutdownDialogClosed = () => {
  if (activePollInterval) {
    clearInterval(activePollInterval);
    activePollInterval = null;
  }
  if (activeProgressInterval) {
    clearInterval(activeProgressInterval);
    activeProgressInterval = null;
  }
};

// 启动安全关机进度指示与状态轮询
const startShutdownPoll = (name: string) => {
  handleShutdownDialogClosed(); // 开启前重置，防止旧定时器重叠

  shutdownVmName.value = name;
  shutdownProgress.value = 10;
  shutdownStatusText.value = '已向系统发送关机信号，正在等待虚拟机关闭...';
  shutdownProgressStatus.value = '';
  showForceShutdownBtn.value = false;
  shutdownDialogVisible.value = true;

  logStore.addLog('info', `关闭虚拟机 ${name}`, '正在等待虚拟机安全关机...');

  let pollCount = 0;
  const maxPoll = 15; // 轮询上限 15 次

  // 进度条平滑自增动画，最大在关闭成功前仅逼近 90%
  activeProgressInterval = setInterval(() => {
    if (shutdownProgress.value < 90) {
      shutdownProgress.value = Math.min(
        90,
        Math.round(shutdownProgress.value + (90 - shutdownProgress.value) * 0.15)
      );
    }
  }, 1000);

  // 定时向后端拉取虚机真实状态进行验证
  activePollInterval = setInterval(async () => {
    pollCount++;
    try {
      const detail = await getVmDetail(name);
      if (detail.state === '关闭') {
        handleShutdownDialogClosed(); // 停止轮询

        shutdownProgress.value = 100;
        shutdownProgressStatus.value = 'success';
        shutdownStatusText.value = '虚拟机已成功安全关闭';
        logStore.addLog('success', `关闭虚拟机 ${name}`, '虚拟机已成功安全关机');

        setTimeout(() => {
          shutdownDialogVisible.value = false;
          fetchData(); // 刷新表格数据
        }, 1200);
      } else if (pollCount >= maxPoll) {
        handleShutdownDialogClosed(); // 达到上限停止轮询

        shutdownProgressStatus.value = 'exception';
        shutdownStatusText.value = '虚拟机未能在规定时间内响应关机信号。这可能是因为虚拟机内部系统未运行或未安装 ACPI 关机支持。你可以关闭本窗口，或执行强制断电。';
        showForceShutdownBtn.value = true;
        logStore.addLog('error', `关闭虚拟机 ${name}`, '虚拟机关机等待超时');
      }
    } catch (error) {
      console.error('轮询虚拟机状态发生异常', error);
    }
  }, 1500);
};
```

---

# 5 系统测试与验证

## 5.1 本地静态编译测试
系统对前端静态页面执行了严格的 TypeScript 语法与编译规范校验。在 [client-web](file:///D:/Code/Other/kvm/client-web) 目录下执行：
```bash
powershell -ExecutionPolicy Bypass -Command "npm run build"
```
系统构建提示 `built in 1.25s` 并顺利输出了前端的打包资源，表明前端对于 Pinia 的 `useLogStore` 以及 `getVmDetail` 的类型和函数参数引用均完全合规。

## 2.2 CentOS 上接口功能性黑盒测试
在 CentOS 真实后端拉起后，我们在物理机本地通过 `curl` 针对核心资源获取接口进行黑盒调用：

### 5.2.1 宿主机监控参数接口测试
```bash
curl -s http://127.0.0.1:8080/api/host/info
```
*   **返回数据**：
    `{"success":true,"message":"操作成功","data":{"hostname":"centos","cpuModel":"x86_64","cpuCount":4,"cpuMHz":3593,"totalMemoryMb":7647,"usedMemoryMb":2327,"freeMemoryMb":5320,"memoryUsagePercent":30,"virtualizationType":"KVM","libvirtVersion":"11.10.0","qemuVersion":"10.1.0","kvmEnabled":true,"connectionUri":"qemu:///system"}}`
*   **结论**：硬件主频、核心数获取精度为 100%，内存利用率解析符合系统预期。

### 5.2.2 存储卷列表接口测试
```bash
curl -s http://127.0.0.1:8080/api/storage/pools/default/volumes
```
*   **返回数据**：
    `{"success":true,"message":"操作成功","data":[{"name":"CorePure64-15.0.iso","path":"/var/lib/libvirt/images/CorePure64-15.0.iso","type":"file","capacityGb":0.0,"allocationGb":0.0},{"name":"cirros-0.5.2-x86_64-disk.img","path":"/var/lib/libvirt/images/cirros-0.5.2-x86_64-disk.img","type":"file","capacityGb":0.1,"allocationGb":0.0},{"name":"ubuntu16.04.7.img","path":"/var/lib/libvirt/images/ubuntu16.04.7.img","type":"file","capacityGb":10.0,"allocationGb":5.4}]}`
*   **结论**：存储卷名称、分配空间、文件物理格式获取精准。

## 5.3 虚拟机正常关机与异常断电流程测试结果
在前端网页控制台上对虚拟机进行操作：
1.  **控制台日志输出详情**：当点击“启动”时，控制台操作面板依次呈现：
    *   `[启动虚拟机 demo] 正在向系统发送启动信号...`
    *   `[启动虚拟机 demo] 正在发送请求`
    *   `[启动虚拟机 demo] 操作成功`
    *   `[启动虚拟机 demo] 启动指令已下发，正在验证虚拟机运行状态...`
    *   `[启动虚拟机 demo] 虚拟机状态已成功转为 [运行中]`
2.  **ACPI 超时降级演练**：对极微型虚拟机 `tinycore-test` 发起关机。由于其无内部 ACPI 响应服务，进度条于 22.5 秒后变红并发出 `虚拟机关机等待超时` 日志。点击弹窗下方的“强制断电”按钮，控制台立刻反馈 `[强制关机虚拟机 tinycore-test] 强制关闭成功`。在宿主机上查询状态，虚拟机已被成功销毁（`virDomainDestroy`），功能验证完美通过。

![图5-1 平台主控看板运行截图](报告模板v5_media/media/image_placeholder.png)
*(图5-1 平台主控看板运行截图)*

![图5-2 虚拟机生命周期控制与控制台日志记录截图](报告模板v5_media/media/image_placeholder.png)
*(图5-2 虚拟机生命周期与日志截图)*

---

# 6 小组分工

本课程设计由小组协作开发完成，具体职责分工如下：

*   **成员 A（学号：XXXXXX）**：负责底层 libvirt API 的 JNA 库映射设计与后端 REST 网关搭建，实现双 Profile 编译配置及 CentOS 服务器端 hosts 解析与 QEMU 能力检测环境系统级调优与服务重启。
*   **成员 B（学号：XXXXXX）**：负责基于 Vue 3 + TypeScript 网页客户端的编写与 FlatLaf 桌面 Swing 端的开发，完成控制台日志追踪面板、关机进度轮询及超时强制断电防抖交互机制的设计与实现。
