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

已在 CentOS 上验证以下功能的接口调用：

- 获取宿主机系统信息（已优化获取主机名逻辑，解决 DNS 反向解析导致超时挂起的问题）
- 查询虚拟机列表与详情
- 虚拟机创建（支持基于已部署的系统镜像或光盘进行创建与定义）
- 虚拟机删除（支持清除虚拟机定义并同步删除磁盘映像文件）
- 虚拟机控制操作（启动、请求关机、强制关闭、暂停、恢复）
- 虚拟网络查询与状态切换（启动和停止默认网络）
- 存储池查询与存储卷明细查看
- 快照全生命周期管理（支持快照创建、列表查询、恢复回滚与彻底删除）

在测试中，对于使用 raw 磁盘格式的默认虚拟机（例如 demo），由于底层存储驱动限制无法支持内部快照，相关快照接口会返回格式不支持的业务错误。目前已在 CentOS 环境中通过新增部署 qcow2 磁盘格式的测试虚拟机，成功验证了快照创建、恢复回滚以及删除的全部生命周期。

---

## Web 客户端部署说明

对于编译打包后的网页端客户端，有两种推荐的 CentOS 部署模式：

### 方案一：嵌入 Spring Boot 静态资源（推荐，单端口部署）

这是最简便的部署方式，不需要额外配置 Web 服务器，且规避了任何跨域问题。

1. 在开发端或编译服务器上，进入 `client-web` 目录打包前端：
   ```bash
   npm run build
   ```
2. 将构建出的 `client-web/dist/` 目录中的全部静态内容，拷贝到 Java 后端资源目录下：
   - 目标位置：`backend/src/main/resources/static/`
3. 执行 Maven 打包命令：
   ```bash
   mvn clean package -DskipTests
   ```
4. 将生成的 `kvm-cloud-backend.jar` 复制并部署到 CentOS 服务器上启动。
5. 启动后，直接使用浏览器访问 CentOS 主机地址：
   ```text
   http://192.168.61.130:8080/
   ```
   即可通过 Spring Boot 内置服务器直接载入网页端控制台，同时共享 API 会话。

### 方案二：独立 Nginx 反向代理部署

1. 在前端目录下执行 `npm run build`，将 `dist/` 目录压缩上传至 CentOS（例如存放于 `/var/www/kvm` 目录）。
2. 在 CentOS 上配置 Nginx 服务（以 `/etc/nginx/conf.d/kvm.conf` 为例）：
   ```nginx
   server {
       listen       80;
       server_name  192.168.61.130;

       # 托管前端静态页面
       location / {
           root   /var/www/kvm;
           index  index.html index.htm;
           try_files $uri $uri/ /index.html;
       }

       # 反向代理后端网关 API
       location /api {
           proxy_pass http://127.0.0.1:8080/api;
           proxy_set_header Host $host;
           proxy_set_header X-Real-IP $remote_addr;
           proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
       }
   }
   ```
3. 在 CentOS 上开放防火墙 80 端口，启动或平滑重启 Nginx 服务。
4. 在开发机中访问 `http://192.168.61.130/` 即可开始使用系统。
