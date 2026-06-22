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

- 获取宿主机系统信息：优化了获取主机名逻辑，降低了域名解析延迟导致的超时问题。
- 查询虚拟机列表与详情
- 虚拟机创建：支持基于系统镜像或光盘定义虚拟机。
- 虚拟机删除：支持清除虚拟机定义并同步删除关联的磁盘文件。
- 虚拟机控制操作：包含启动、关机、重启、强制断电、暂停与恢复。
- 虚拟网络查询与状态切换：支持启动与停止默认网络。
- 存储池查询与存储卷明细查看
- 快照生命周期管理：支持快照创建、列表查询、恢复与删除。

在测试中，由于底层存储驱动限制，使用 raw 磁盘格式的虚拟机不支持创建快照，调用接口时会返回格式不支持的错误。目前已在 CentOS 环境中新增部署 qcow2 磁盘格式的测试虚拟机，验证了快照创建、恢复与删除功能。

---

## Web 客户端部署说明

对于打包后的 Web 客户端，有两种部署模式：

### 方案一：嵌入 Spring Boot 静态资源部署

此方式无需额外配置 Web 服务器，避免了跨域配置。

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
    即可通过 Spring Boot 服务访问 Web 客户端，并共享接口服务。

### 方案二：独立 Nginx 反向代理部署

1. 在前端目录下执行 `npm run build`，将 `dist/` 目录打包上传至 CentOS（如存放于 `/var/www/kvm` 目录）。
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

        # 反向代理后端网关 API 及 WebSocket
        location /api {
            proxy_pass http://127.0.0.1:8080/api;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

            # 开启 WebSocket 双向流协议升级支持，用于 VNC 网页直连
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_read_timeout 3600s; # 防止控制台闲置超时断开
        }
    }
   ```
3. 在 CentOS 上开放防火墙 80 端口，启动或平滑重启 Nginx 服务。
4. 在开发机中访问 `http://192.168.61.130/` 即可开始使用系统。
