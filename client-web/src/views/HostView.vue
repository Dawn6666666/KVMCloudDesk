<template>
  <div class="host-container">
    <el-card v-loading="loading" class="info-card">
      <template #header>
        <div class="card-header">
          <span>宿主机物理及配置参数</span>
          <el-button size="small" type="primary" :icon="Refresh" @click="fetchData">刷新</el-button>
        </div>
      </template>

      <div v-if="hostInfo" class="content-box">
        <el-row :gutter="20" class="metric-row">
          <el-col :span="8">
            <div class="metric-block glass-panel">
              <span class="metric-label">主机名称</span>
              <span class="metric-value">{{ hostInfo.hostname }}</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="metric-block glass-panel">
              <span class="metric-label">物理内存</span>
              <span class="metric-value">{{ (hostInfo.totalMemoryMb / 1024).toFixed(2) }} GB</span>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="metric-block glass-panel">
              <span class="metric-label">处理器规格</span>
              <span class="metric-value">{{ hostInfo.cpuCount }} 核心 ({{ hostInfo.cpuMHz }} MHz)</span>
            </div>
          </el-col>
        </el-row>

        <el-descriptions border :column="2" class="desc-list">
          <el-descriptions-item label="CPU 型号">
            {{ hostInfo.cpuModel }}
          </el-descriptions-item>
          <el-descriptions-item label="内存空闲 / 已用">
            {{ (hostInfo.freeMemoryMb / 1024).toFixed(2) }} GB / {{ (hostInfo.usedMemoryMb / 1024).toFixed(2) }} GB (已使用 {{ hostInfo.memoryUsagePercent }}%)
          </el-descriptions-item>
          <el-descriptions-item label="虚拟化管理程序">
            {{ hostInfo.virtualizationType }}
          </el-descriptions-item>
          <el-descriptions-item label="KVM 加速支持">
            <el-tag :type="hostInfo.kvmEnabled ? 'success' : 'danger'" size="small">
              {{ hostInfo.kvmEnabled ? '支持并开启' : '未开启或不支持' }}
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
      </div>
      
      <el-empty v-else description="无法加载宿主机监控参数，请确认后端服务是否正常连接" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import { getHostInfo } from '@/api/kvm';
import type { HostInfoDto } from '@/types/kvm';

const hostInfo = ref<HostInfoDto | null>(null);
const loading = ref(false);

const fetchData = async () => {
  loading.value = true;
  try {
    const data = await getHostInfo();
    hostInfo.value = data;
  } catch (error) {
    console.error('获取宿主机信息失败', error);
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.host-container {
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-box {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.metric-row {
  margin-bottom: 8px;
}

.metric-block {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  background: rgba(255, 255, 255, 0.02);
}

.metric-label {
  font-size: 12px;
  color: var(--text-secondary);
}

.metric-value {
  font-family: var(--font-title);
  font-size: 20px;
  font-weight: 700;
  color: var(--text-primary);
}

.desc-list {
  background-color: transparent !important;
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
