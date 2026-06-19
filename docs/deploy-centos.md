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
