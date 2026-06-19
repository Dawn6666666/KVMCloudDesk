# Windows 运行说明

要求：

- JDK 21
- Maven
- Node.js (仅用于 Web 客户端)

构建系统：

```bash
mvn clean package -DskipTests
```

启动 mock 后端：

```bash
java -jar backend/target/kvm-cloud-backend.jar --spring.profiles.active=mock
```

启动 Swing 客户端：

```bash
java -jar client-swing/target/kvm-cloud-client.jar
```

启动 Web 客户端：

在 `client-web` 目录下执行：

```bash
npm install
npm run dev
```

运行后，通过浏览器访问 `http://localhost:5173`。Vite 开发服务器会自动将以 `/api` 开头的网络请求代理转发至本地后端的 8080 端口。

如果需要将本地前端与远程 CentOS 上运行的真实后端进行联调，可以直接修改 [vite.config.ts](file:///d:/Code/Other/kvm/client-web/vite.config.ts) 中的代理配置：
```ts
server: {
  proxy: {
    '/api': {
      target: 'http://192.168.61.130:8080',
      changeOrigin: true
    }
  }
}
```
修改完成后，再次启动前端，在 Windows 开发环境上即可直接对远程服务器上的真实 libvirt 后端服务进行调试。

注意事项：

- Windows 本地开发环境不要启动 `libvirt` 配置文件，不需要连接真实的虚拟化管理程序。
- 启动网页端前，请保证本地 mock 后端已在 8080 端口正常提供服务。
