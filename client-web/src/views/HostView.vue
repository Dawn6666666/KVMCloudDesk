<template>
  <div class="host-container" v-loading="loading">
    <div class="host-header glass-panel">
      <h2>宿主机物理底座与系统运行报告</h2>
      <el-button type="primary" :icon="Refresh" @click="fetchData(true)">手动刷新</el-button>
    </div>

    <div v-if="hostInfo" class="host-content">
      <!-- Section 1: Hardware Topology -->
      <el-card class="info-card glass-panel" header="处理器与内存硬件拓扑">
        <el-descriptions border :column="2">
          <el-descriptions-item label="CPU 处理器型号" :span="2">
            <strong>{{ hostInfo.cpuModel }}</strong>
          </el-descriptions-item>
          <el-descriptions-item label="处理器拓扑架构">
            {{ hostInfo.cpuSockets }} 插槽 &times; {{ hostInfo.cpuCores }} 核心
          </el-descriptions-item>
          <el-descriptions-item label="每核心超线程数">
            {{ hostInfo.cpuThreads }} 线程
          </el-descriptions-item>
          <el-descriptions-item label="逻辑处理器总数">
            <div class="metric-progress-cell">
              <span>{{ hostInfo.cpuCount }} 逻辑核心</span>
              <div class="progress-wrap">
                <span class="pct-text">实时占用 {{ hostInfo.cpuUsagePercent }}%</span>
                <el-progress :percentage="hostInfo.cpuUsagePercent" :stroke-width="8" status="exception" />
              </div>
            </div>
          </el-descriptions-item>
          <el-descriptions-item label="NUMA 架构节点数">
            {{ hostInfo.numaNodes }} NUMA节点
          </el-descriptions-item>
          <el-descriptions-item label="主频规格">
            {{ hostInfo.cpuMHz }} MHz
          </el-descriptions-item>
          <el-descriptions-item label="物理内存配置" :span="2">
            <div class="metric-progress-cell">
              <span>已用 {{ (hostInfo.usedMemoryMb / 1024).toFixed(2) }} GB / 空闲 {{ (hostInfo.freeMemoryMb / 1024).toFixed(2) }} GB (共 {{ (hostInfo.totalMemoryMb / 1024).toFixed(2) }} GB)</span>
              <div class="progress-wrap">
                <span class="pct-text">内存已用 {{ hostInfo.memoryUsagePercent }}%</span>
                <el-progress :percentage="hostInfo.memoryUsagePercent" :stroke-width="8" status="warning" />
              </div>
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Section 2: OS & Status -->
      <el-card class="info-card glass-panel" header="操作系统与系统运行状态">
        <el-descriptions border :column="2">
          <el-descriptions-item label="宿主机名称">
            {{ hostInfo.hostname }}
          </el-descriptions-item>
          <el-descriptions-item label="操作系统名称">
            {{ hostInfo.osName }}
          </el-descriptions-item>
          <el-descriptions-item label="内核版本">
            {{ hostInfo.osKernel }}
          </el-descriptions-item>
          <el-descriptions-item label="系统平均负载">
            1分钟 Load Average: <strong>{{ hostInfo.systemLoadAverage }}</strong>
          </el-descriptions-item>
          <el-descriptions-item label="连续运行时间" :span="2">
            <strong>{{ hostInfo.uptime }}</strong>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <!-- Section 3: Virtualization & API -->
      <el-card class="info-card glass-panel" header="虚拟化底座与接口参数">
        <el-descriptions border :column="2">
          <el-descriptions-item label="底座管理程序">
            {{ hostInfo.virtualizationType }} (QEMU-KVM)
          </el-descriptions-item>
          <el-descriptions-item label="KVM 加速支持">
            <el-tag :type="hostInfo.kvmEnabled ? 'success' : 'danger'" size="small">
              {{ hostInfo.kvmEnabled ? '开启硬件辅助虚拟化' : '未开启或不支持' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="Libvirt API 版本">
            {{ hostInfo.libvirtVersion }}
          </el-descriptions-item>
          <el-descriptions-item label="QEMU 模拟器版本">
            {{ hostInfo.qemuVersion }}
          </el-descriptions-item>
          <el-descriptions-item label="后端连接 URI" :span="2">
            <code class="uri-code">{{ hostInfo.connectionUri }}</code>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>

    <el-empty v-else description="无法加载宿主机监控参数，请确认后端服务是否正常连接" />
  </div>
</template>



<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import { getHostInfo } from '@/api/kvm';
import type { HostInfoDto } from '@/types/kvm';

const hostInfo = ref<HostInfoDto | null>(null);
const loading = ref(false);
let timer: any = null;

const fetchData = async (showLoading = true) => {
  if (showLoading) {
    loading.value = true;
  }
  try {
    const data = await getHostInfo();
    hostInfo.value = data;
  } catch (error) {
    console.error('获取宿主机信息失败', error);
  } finally {
    if (showLoading) {
      loading.value = false;
    }
  }
};

onMounted(() => {
  fetchData(true);
  timer = setInterval(() => {
    fetchData(false);
  }, 3000);
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
});
</script>

<style scoped>
.host-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.host-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.01);
}

.host-header h2 {
  margin: 0;
  font-family: var(--font-title);
  font-size: 18px;
  font-weight: 600;
  color: var(--text-primary);
}

.host-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.info-card {
  width: 100%;
}

.metric-progress-cell {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  flex-wrap: wrap;
  gap: 12px;
}

.progress-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

.pct-text {
  font-size: 12px;
  color: var(--text-secondary);
}

:deep(.el-descriptions__table) {
  background-color: transparent !important;
  border-collapse: collapse;
}

:deep(.el-descriptions__label) {
  background-color: rgba(255, 255, 255, 0.02) !important;
  color: var(--text-secondary) !important;
  font-weight: 600 !important;
  width: 180px;
}

:deep(.el-descriptions__content) {
  color: var(--text-primary) !important;
  background-color: transparent !important;
}

.uri-code {
  font-family: 'Consolas', monospace;
  background-color: rgba(255, 255, 255, 0.05);
  padding: 2px 8px;
  border-radius: 4px;
  color: #a78bfa;
}
</style>
