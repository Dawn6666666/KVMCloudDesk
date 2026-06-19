<template>
  <el-container class="app-container">
    <el-aside width="240px" class="aside-menu glass-panel">
      <div class="logo">
        <el-icon :size="24" color="var(--color-primary)"><Monitor /></el-icon>
        <span class="logo-text">KVM Cloud Desk</span>
      </div>
      
      <el-menu :default-active="activeRoute" router>
        <el-menu-item index="/">
          <el-icon><Monitor /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/host">
          <el-icon><Cpu /></el-icon>
          <span>宿主机</span>
        </el-menu-item>
        <el-menu-item index="/vms">
          <el-icon><Grid /></el-icon>
          <span>虚拟机</span>
        </el-menu-item>
        <el-menu-item index="/images">
          <el-icon><Folder /></el-icon>
          <span>系统镜像</span>
        </el-menu-item>
        <el-menu-item index="/networks">
          <el-icon><Share /></el-icon>
          <span>虚拟网络</span>
        </el-menu-item>
        <el-menu-item index="/snapshots">
          <el-icon><Camera /></el-icon>
          <span>快照管理</span>
        </el-menu-item>
        <el-menu-item index="/storage">
          <el-icon><DataAnalysis /></el-icon>
          <span>存储管理</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    
    <el-container class="main-container">
      <el-header class="app-header glass-panel">
        <div class="header-left">
          <h2 class="page-title">{{ pageTitle }}</h2>
        </div>
        <div class="header-right">
          <div class="connection-status">
            <span :class="['status-dot', backendStore.connected ? 'active' : 'error']"></span>
            <span class="status-text">{{ backendStore.connected ? '已连接后端' : '后端连接断开' }}</span>
          </div>
          <el-button 
            type="primary" 
            :loading="backendStore.loading" 
            circle 
            :icon="Refresh"
            @click="refreshHostInfo"
          />
        </div>
      </el-header>
      
      <el-main class="app-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
      
      <div :class="['console-panel', { collapsed: isConsoleCollapsed }]">
        <div class="console-header" @click="isConsoleCollapsed = !isConsoleCollapsed">
          <div class="console-title">
            <el-icon><List /></el-icon>
            <span>操作日志控制台</span>
            <el-tag size="small" type="info" round class="log-count">
              {{ logStore.logs.length }}
            </el-tag>
          </div>
          <div class="console-actions">
            <el-button 
              size="small" 
              type="text" 
              :icon="Delete" 
              @click.stop="logStore.clearLogs"
            >
              清空
            </el-button>
            <span class="collapse-toggle">
              {{ isConsoleCollapsed ? '展开' : '折叠' }}
            </span>
          </div>
        </div>
        <div ref="logContainer" class="console-body">
          <div v-if="logStore.logs.length === 0" class="empty-log">
            暂无操作记录
          </div>
          <div 
            v-for="log in logStore.logs" 
            :key="log.id" 
            :class="['log-line', log.type]"
          >
            <span class="log-time">[{{ log.timestamp }}]</span>
            <span class="log-action">[{{ log.action }}]</span>
            <span class="log-detail">{{ log.detail }}</span>
          </div>
        </div>
      </div>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import { useBackendStore } from '@/stores/backendStore';
import { useLogStore } from '@/stores/logStore';
import { 
  Monitor, Cpu, Grid, Folder, Share, Camera, DataAnalysis, Refresh, List, Delete 
} from '@element-plus/icons-vue';

const route = useRoute();
const backendStore = useBackendStore();
const logStore = useLogStore();

const isConsoleCollapsed = ref(false);
const logContainer = ref<HTMLElement | null>(null);

const activeRoute = computed(() => route.path);

const pageTitle = computed(() => {
  switch (route.name) {
    case 'Dashboard': return '平台仪表盘';
    case 'Host': return '宿主机物理信息';
    case 'VMs': return '云虚拟机实例';
    case 'Images': return '系统镜像目录';
    case 'Networks': return '虚拟局域网';
    case 'Snapshots': return '云虚拟机快照';
    case 'Storage': return '存储池与卷';
    default: return 'KVM 云管理平台';
  }
});

const refreshHostInfo = async () => {
  try {
    await backendStore.fetchHostInfo();
  } catch (e) {
    // Ignore error
  }
};

onMounted(() => {
  refreshHostInfo();
});

watch(() => logStore.logs.length, () => {
  nextTick(() => {
    if (logContainer.value) {
      logContainer.value.scrollTop = 0;
    }
  });
});
</script>

<style scoped>
.app-container {
  height: 100vh;
  width: 100vw;
  background-color: var(--bg-base);
  display: flex;
}

.aside-menu {
  margin: 12px 6px 12px 12px;
  padding: 16px 0;
  display: flex;
  flex-direction: column;
  border-radius: 16px;
  background: var(--bg-surface);
}

.logo {
  display: flex;
  align-items: center;
  padding: 0 20px 20px 20px;
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 12px;
}

.logo-text {
  font-family: var(--font-title);
  font-size: 18px;
  font-weight: 700;
  margin-left: 10px;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.main-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  flex: 1;
  overflow: hidden;
}

.app-header {
  margin: 12px 12px 6px 6px;
  height: 64px !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  border-radius: 16px;
  background: var(--bg-surface);
}

.page-title {
  font-family: var(--font-title);
  font-size: 18px;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.connection-status {
  display: flex;
  align-items: center;
  font-size: 13px;
  color: var(--text-secondary);
}

.status-text {
  font-weight: 500;
}

.app-main {
  flex: 1;
  overflow-y: auto;
  padding: 6px 12px 6px 6px;
}

.console-panel {
  margin: 6px 12px 12px 6px;
  border: 1px solid var(--border-color);
  background: #fbfaf7;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  height: 200px;
  overflow: hidden;
}

.console-panel.collapsed {
  height: 48px;
}

.console-header {
  height: 48px;
  min-height: 48px;
  padding: 0 20px;
  background: rgba(44, 37, 32, 0.03);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  user-select: none;
}

.console-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: var(--font-title);
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
}

.log-count {
  font-family: var(--font-body);
}

.console-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  color: var(--text-secondary);
}

.collapse-toggle {
  opacity: 0.7;
}

.console-body {
  flex: 1;
  padding: 12px 20px;
  overflow-y: auto;
  font-family: 'Consolas', 'Courier New', monospace;
  font-size: 12px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.empty-log {
  color: var(--text-muted);
  text-align: center;
  margin-top: 20px;
  font-style: italic;
}

.log-line {
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
  display: flex;
  gap: 8px;
}

.log-time {
  color: var(--text-muted);
  flex-shrink: 0;
}

.log-action {
  color: var(--color-info);
  font-weight: 600;
  flex-shrink: 0;
}

.log-line.success .log-action {
  color: var(--color-success);
}

.log-line.error .log-action {
  color: var(--color-danger);
}

.log-detail {
  color: var(--text-primary);
}

.log-line.error .log-detail {
  color: var(--color-danger);
}

.log-line.success .log-detail {
  color: var(--color-success);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
