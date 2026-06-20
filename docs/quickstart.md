# KVM 云平台管理系统快速入门指南

本指南用于在 Windows 环境下搭建模拟调试环境，或将系统打包部署至 CentOS 环境中运行。

---

## 0. 本地开发与打包环境路径

由于 Windows 宿主机上未配置 Maven 全局变量，且默认 `JAVA_HOME` 指向 JDK 17，需使用以下绝对路径进行打包：
- **JDK 21 绝对路径**：`C:\Users\24831\.jdks\ms-21.0.10`
- **Maven 绝对路径**：`D:\Jetbrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd`

---

## 1. Windows 本地模拟运行

此模式不需要 Linux 环境和 libvirt，直接在 Windows 本地运行前后端配合假数据调试界面逻辑。

### 步骤 1：编译后端

在 Windows 宿主机根目录下打开 PowerShell 执行以下命令：
```powershell
powershell -Command '$env:JAVA_HOME = "C:\Users\24831\.jdks\ms-21.0.10"; & "D:\Jetbrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" clean package -DskipTests'
```

### 步骤 2：启动后端

```bash
java -jar backend/target/kvm-cloud-backend.jar --spring.profiles.active=mock
```
后端进程将启动并监听在 `http://127.0.0.1:8080`。

### 步骤 3：启动 Web 前端

打开一个新终端：
```bash
cd client-web
npm install
npm run dev
```
启动后通过浏览器访问 `http://localhost:5173/` 即可开始开发调试。

> **注意**：`vite.config.ts` 中 `/api` 代理默认指向 `http://192.168.61.130:8080`（CentOS 远程后端）。若要在本地 mock 模式下开发，需将 `target` 改为 `http://127.0.0.1:8080`。

### 步骤 4：启动 Swing 客户端

打开另一个新终端：
```bash
java -jar client-swing/target/kvm-cloud-client.jar
```
Swing 客户端默认连接 `http://127.0.0.1:8080`，可通过启动参数覆盖：
```bash
java -jar client-swing/target/kvm-cloud-client.jar --backend.url=http://192.168.61.130:8080
```

---

## 2. CentOS 环境合并部署

此模式将 Web 客户端编译后的静态文件放入后端静态托管目录，统一部署至 CentOS 服务器，由后端端口 `8080`同时提供 Web 页面与 API 服务。

### 步骤 1：宿主机环境配置

登录 CentOS 虚拟机，确保执行以下修复防止系统级超时和版本获取异常：
```bash
# 1. 将本机主机名绑定至回环地址，降低解析延迟
echo "127.0.0.1 centos" >> /etc/hosts
echo "::1 centos" >> /etc/hosts

# 2. 移除旧版 QEMU 执行文件，以使系统调用合规的 QEMU 10.1.0
mv /usr/local/bin/qemu-system-x86_64 /usr/local/bin/qemu-system-x86_64.bak
systemctl restart virtqemud
```

### 步骤 2：打包前端并同步至后端静态目录

```powershell
# 1. 编译前端静态资源
cd client-web
npm run build
cd ..

# 2. 清空并拷贝静态包到 Java 资源 static 文件夹
Remove-Item -Path 'D:\Code\Other\kvm\backend\src\main\resources\static\*' -Recurse -Force
Copy-Item -Path 'D:\Code\Other\kvm\client-web\dist\*' -Destination 'D:\Code\Other\kvm\backend\src\main\resources\static' -Recurse -Force
```

### 步骤 3：编译后端项目

```powershell
powershell -Command '$env:JAVA_HOME = "C:\Users\24831\.jdks\ms-21.0.10"; & "D:\Jetbrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" clean package -DskipTests'
```

### 步骤 4：上传并启动服务

```powershell
# 1. 上传新生成的 jar 运行包
scp backend/target/kvm-cloud-backend.jar centos:/tmp/kvm-cloud-backend.jar

# 2. 登录并重启服务
ssh centos "pkill -f kvm-cloud-backend.jar || true; sleep 3; nohup java -jar /tmp/kvm-cloud-backend.jar --spring.profiles.active=libvirt > /tmp/kvm.log 2>&1 < /dev/null & sleep 3"
```

### 步骤 5：访问系统

用浏览器直接访问 CentOS 的 8080 端口：
**[http://192.168.61.130:8080/](http://192.168.61.130:8080/)**

即可访问系统 Web 管理控制台。
