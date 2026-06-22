package com.example.kvm.backend.config;

import com.example.kvm.backend.service.VmService;
import com.example.kvm.common.dto.VmInfoDto;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Component
public class VncProxyWebSocketHandler extends BinaryWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(VncProxyWebSocketHandler.class);
    private final VmService vmService;

    public VncProxyWebSocketHandler(VmService vmService) {
        this.vmService = vmService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri().getPath();
        String vmName = path.substring(path.lastIndexOf('/') + 1);
        log.info("收到虚拟机 {} 的 VNC WebSocket 连接请求", vmName);

        VmInfoDto vm;
        try {
            vm = vmService.getVm(vmName);
        } catch (Exception e) {
            log.error("获取虚拟机信息失败：{}", vmName, e);
            session.close(CloseStatus.BAD_DATA);
            return;
        }

        if (!"运行".equals(vm.state)) {
            log.warn("虚拟机 {} 当前未运行，无法连接 VNC", vmName);
            session.close(new CloseStatus(4001, "虚拟机未运行"));
            return;
        }

        if (vm.vncPort == null || vm.vncPort <= 0) {
            log.warn("虚拟机 {} 未分配 VNC 端口", vmName);
            session.close(new CloseStatus(4002, "未分配 VNC 端口"));
            return;
        }

        log.info("正在建立到本地 VNC 端口 {} 的连接", vm.vncPort);
        Socket socket;
        try {
            socket = new Socket("localhost", vm.vncPort);
        } catch (IOException e) {
            log.error("连接到 VNC 物理端口 {} 失败", vm.vncPort, e);
            session.close(CloseStatus.SERVER_ERROR);
            return;
        }

        session.getAttributes().put("socket", socket);

        // 启动后台线程异步读取本地 VNC 端口字节，转发给前端 Websocket
        Thread readerThread = new Thread(() -> {
            byte[] buffer = new byte[8192];
            try (InputStream in = socket.getInputStream()) {
                int len;
                while (!Thread.currentThread().isInterrupted() && (len = in.read(buffer)) != -1) {
                    if (session.isOpen()) {
                        synchronized (session) {
                            session.sendMessage(new BinaryMessage(buffer, 0, len, true));
                        }
                    } else {
                        break;
                    }
                }
            } catch (IOException e) {
                log.debug("VNC 端口读取异常或连接关闭", e);
            } finally {
                cleanup(session, socket);
            }
        });
        readerThread.setName("vnc-reader-" + vmName);
        readerThread.start();
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        Socket socket = (Socket) session.getAttributes().get("socket");
        if (socket != null && !socket.isClosed()) {
            try {
                ByteBuffer payload = message.getPayload();
                byte[] bytes = new byte[payload.remaining()];
                payload.get(bytes);
                OutputStream out = socket.getOutputStream();
                out.write(bytes);
                out.flush();
            } catch (IOException e) {
                log.error("向 VNC 物理端口写入数据异常", e);
                cleanup(session, socket);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Socket socket = (Socket) session.getAttributes().remove("socket");
        log.info("VNC WebSocket 连接已断开，状态：{}", status);
        if (socket != null) {
            cleanup(session, socket);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("VNC WebSocket 传输异常", exception);
        Socket socket = (Socket) session.getAttributes().remove("socket");
        cleanup(session, socket);
    }

    private void cleanup(WebSocketSession session, Socket socket) {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                log.warn("关闭 VNC Socket 发生异常", e);
            }
        }
        if (session != null && session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.warn("关闭 VNC WebSocket Session 发生异常", e);
            }
        }
    }
}
