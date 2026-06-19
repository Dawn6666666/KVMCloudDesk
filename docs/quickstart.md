# KVM 云平台管理系统快速入门指南 (Quickstart)

本指南旨在指导开发者最快地在 Windows 本地拉起 MOCK 调试环境，或将前后端合并打包部署至 CentOS 真实硬件环境中运行。

---

## 0. 本地开发与打包环境路径

由于 Windows 宿主机上未配置 Maven 全局变量，且默认 `JAVA_HOME` 指向 JDK 17，我们需使用以下绝对路径进行打包：
- **JDK 21 绝对路径**：`C:\Users\24831\.jdks\ms-21.0.10`
- **Maven 绝对路径**：`D:\Jetbrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd`

---

## 1. Windows 本地 mock 运行（无虚拟化依赖）

此模式不需要 Linux 环境和 libvirt，直接在 Windows 本地运行前后端配合假数据调试界面逻辑。

### 步骤 1：编译并启动后端 (mock 模式)
在 Windows 宿主机根目录下打开 PowerShell 执行以下命令：
```powershell
# 1. 编译打包后端
powershell -Command '$env:JAVA_HOME = "C:\Users\24831\.jdks\ms-21.0.10"; & "D:\Jetbrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" clean package -DskipTests'

# 2. 以 mock 配置文件运行服务
java -jar backend/target/kvm-cloud-backend.jar --spring.profiles.active=mock
```
后端进程将启动并监听在 `http://127.0.0.1:8080`。

### 步骤 2：启动前端网页客户端
打开一个新的终端进入 [client-web](file:///D:/Code/Other/kvm/client-web) 目录：
```bash
cd client-web
npm install
npm run dev
```
启动后通过浏览器访问 `http://localhost:5173/` 即可开始开发调试。

---

## 2. CentOS 真实 libvirt 环境合并部署（生产模式）

此模式将前端网页合并编译在 Java 后端的静态托管库中，发布到 CentOS 服务器上，通过单端口 `8080` 同时对外面板展示和 API 提供。

### 步骤 1：宿主机环境优化配置 (CentOS 服务器端)
登录 CentOS 虚拟机，确保执行以下修复防止系统级超时和版本获取异常：
```bash
# 1. 将本机主机名 centos 绑定至回环地址，以防虚拟机启动和关机出现 15 秒超时
echo "127.0.0.1 centos" >> /etc/hosts
echo "::1 centos" >> /etc/hosts

# 2. 屏蔽不兼容的旧版 QEMU 4.1.0 模拟器程序，使系统自动探测默认合规的 QEMU 10.1.0
mv /usr/local/bin/qemu-system-x86_64 /usr/local/bin/qemu-system-x86_64.bak
systemctl restart virtqemud
```

### 步骤 2：打包前端并同步至后端静态库 (Windows 开发端)
在 Windows 宿主机中执行以下命令（打包 Vue/TS 并将其拷贝至 Spring Boot 资源中）：
```powershell
# 1. 编译前端静态资源
cd client-web
npm run build
cd ..

# 2. 清空并拷贝静态包到 Java 资源 static 文件夹
Remove-Item -Path 'D:\Code\Other\kvm\backend\src\main\resources\static\*' -Recurse -Force
Copy-Item -Path 'D:\Code\Other\kvm\client-web\dist\*' -Destination 'D:\Code\Other\kvm\backend\src\main\resources\static' -Recurse -Force
```

### 步骤 3：编译合并后的后端 jar 包 (Windows 开发端)
```powershell
powershell -Command '$env:JAVA_HOME = "C:\Users\24831\.jdks\ms-21.0.10"; & "D:\Jetbrains\IntelliJ IDEA 2025.2.4\plugins\maven\lib\maven3\bin\mvn.cmd" clean package -DskipTests'
```

### 步骤 4：上传并拉起后台服务 (CentOS 运行端)
```powershell
# 1. 上传新生成的 jar 运行包
scp backend/target/kvm-cloud-backend.jar centos:/tmp/kvm-cloud-backend.jar

# 2. 登录并重启服务（使用 nohup 后台持久运行）
ssh centos "pkill -f kvm-cloud-backend.jar || true; sleep 3; nohup java -jar /tmp/kvm-cloud-backend.jar --spring.profiles.active=libvirt > /tmp/kvm.log 2>&1 < /dev/null & sleep 3"
```

### 步骤 5：访问系统
用浏览器直接访问 CentOS 的 8080 端口服务：
**[http://192.168.61.130:8080/](http://192.168.61.130:8080/)**
即可直接运行运行了操作日志闭环和状态轮询指示的 KVM 网页管理控制台。
