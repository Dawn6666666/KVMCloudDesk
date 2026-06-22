<template>
  <div class="vnc-container">
    <div class="vnc-status-bar glass-panel">
      <span class="vm-title">虚拟机实例 VNC 控制台：{{ vmName }}</span>
      <div class="status-indicator">
        <span :class="['status-dot', status]"></span>
        <span class="status-text">{{ statusText }}</span>
      </div>
      <div class="actions-group" style="margin-left: auto; display: flex; gap: 8px;">
        <el-button 
          v-if="status === 'connected'"
          size="small" 
          type="warning" 
          plain
          @click="sendCAD"
        >
          发送 Ctrl+Alt+Del
        </el-button>
        <el-button 
          v-if="status === 'connected'"
          size="small" 
          type="info" 
          plain
          @click="toggleFullscreen"
          :icon="FullScreen"
        >
          全屏窗口
        </el-button>
        <el-button 
          size="small" 
          type="primary" 
          :icon="Refresh" 
          @click="reconnect" 
        >
          重新连接
        </el-button>
      </div>
    </div>
    <div class="vnc-screen-wrapper" ref="vncWrapper">
      <div class="vnc-screen" ref="screenDiv"></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { useRoute } from 'vue-router';
import { Refresh, FullScreen } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
// @ts-ignore
import RFB from '@novnc/novnc';

const route = useRoute();
const vmName = ref(route.params.name as string);
const screenDiv = ref<HTMLDivElement | null>(null);
const vncWrapper = ref<HTMLDivElement | null>(null);
const status = ref('disconnected');
const statusText = ref('未连接');
let rfb: any = null;

const sendCAD = () => {
  if (rfb) {
    try {
      rfb.sendCtrlAltDel();
      ElMessage.success('系统热键发送成功');
    } catch (e: any) {
      ElMessage.error('热键发送失败：' + e.message);
    }
  }
};

const toggleFullscreen = () => {
  if (!vncWrapper.value) return;
  if (!document.fullscreenElement) {
    vncWrapper.value.requestFullscreen().catch(err => {
      ElMessage.error(`无法开启全屏: ${err.message}`);
    });
  } else {
    document.exitFullscreen();
  }
};

const connect = () => {
  if (!screenDiv.value) return;
  status.value = 'connecting';
  statusText.value = '正在连接虚拟机内部画面...';

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  const wsUrl = `${protocol}//${window.location.host}/api/vnc-proxy/${vmName.value}`;

  try {
    rfb = new RFB(screenDiv.value, wsUrl);
    
    rfb.addEventListener('connect', () => {
      status.value = 'connected';
      statusText.value = '连接成功';
      // 自动适应屏幕大小缩放
      rfb.scaleViewport = true;
      rfb.resizeSession = true;
    });

    rfb.addEventListener('disconnect', (e: any) => {
      status.value = 'disconnected';
      statusText.value = e.detail.clean ? '控制台会话已结束' : '连接异常中断或虚拟机未开启';
    });

    rfb.addEventListener('credentialsrequired', () => {
      rfb.sendCredentials({ password: '' });
    });
  } catch (error) {
    status.value = 'disconnected';
    statusText.value = '无法初始化控制台连接';
    console.error(error);
  }
};

const reconnect = () => {
  if (rfb) {
    try {
      rfb.disconnect();
    } catch (e) {}
    rfb = null;
  }
  connect();
};

onMounted(() => {
  connect();
});

onUnmounted(() => {
  if (rfb) {
    try {
      rfb.disconnect();
    } catch (e) {}
  }
});
</script>

<style scoped>
.vnc-container {
  display: flex;
  flex-direction: column;
  width: 100vw;
  height: 100vh;
  background-color: #0c0f16;
  color: #e2e8f0;
  overflow: hidden;
  font-family: var(--font-title), sans-serif;
}

.vnc-status-bar {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 12px 24px;
  background: rgba(26, 31, 46, 0.8);
  backdrop-filter: blur(16px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  z-index: 10;
}

.vm-title {
  font-size: 16px;
  font-weight: 600;
  color: #f1f5f9;
}

.status-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 255, 255, 0.03);
  padding: 4px 12px;
  border-radius: 20px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  transition: background-color 0.3s;
}

.status-dot.connected {
  background-color: #10b981;
  box-shadow: 0 0 8px #10b981;
}

.status-dot.connecting {
  background-color: #f59e0b;
  box-shadow: 0 0 8px #f59e0b;
  animation: pulse 1.5s infinite;
}

.status-dot.disconnected {
  background-color: #ef4444;
  box-shadow: 0 0 8px #ef4444;
}

.status-text {
  font-size: 13px;
  color: #94a3b8;
}

.vnc-screen-wrapper {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
  background: #07090e;
  overflow: auto;
}

.vnc-screen {
  background-color: #000000;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.6);
  border: 1px solid rgba(255, 255, 255, 0.05);
}

:deep(.vnc-screen > div) {
  margin: 0 auto;
}

@keyframes pulse {
  0% {
    opacity: 0.6;
  }
  50% {
    opacity: 1;
  }
  100% {
    opacity: 0.6;
  }
}
</style>
