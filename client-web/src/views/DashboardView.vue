<template>
  <div class="dashboard-container">
    <!-- Top Stats Cards -->
    <el-row :gutter="20" class="stat-cards">
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card-content">
            <span class="stat-title">虚拟机总数</span>
            <span class="stat-value">{{ vms.length }}</span>
            <span class="stat-desc">已注册的虚拟主机实例</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card-content">
            <span class="stat-title">运行中实例</span>
            <span class="stat-value text-success">{{ runningVmsCount }}</span>
            <span class="stat-desc">当前正在开机运行的虚拟机</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card-content">
            <span class="stat-title">存储池容量</span>
            <span class="stat-value text-info">{{ totalStoragePoolCapacity.toFixed(1) }} GB</span>
            <span class="stat-desc">所有活跃存储池的总存储容量</span>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div class="stat-card-content">
            <span class="stat-title">宿主机内核</span>
            <span class="stat-value text-purple">{{ hostInfo?.cpuCount || 0 }} 核</span>
            <span class="stat-desc">{{ hostInfo?.cpuModel || '未知 CPU' }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ECharts Row -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="8">
        <el-card header="内存使用率" shadow="hover">
          <div ref="memoryChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="虚拟机状态统计" shadow="hover">
          <div ref="statusChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="存储池容量分布" shadow="hover">
          <div ref="storageChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from 'vue';
import * as echarts from 'echarts';
import { getHostInfo, getVms, getStoragePools } from '@/api/kvm';
import type { HostInfoDto, VmInfoDto, StoragePoolInfoDto } from '@/types/kvm';

const hostInfo = ref<HostInfoDto | null>(null);
const vms = ref<VmInfoDto[]>([]);
const storagePools = ref<StoragePoolInfoDto[]>([]);

const memoryChartRef = ref<HTMLElement | null>(null);
const statusChartRef = ref<HTMLElement | null>(null);
const storageChartRef = ref<HTMLElement | null>(null);

let memoryChart: echarts.ECharts | null = null;
let statusChart: echarts.ECharts | null = null;
let storageChart: echarts.ECharts | null = null;

const runningVmsCount = computed(() => {
  return vms.value.filter(vm => vm.state === '运行').length;
});

const totalStoragePoolCapacity = computed(() => {
  return storagePools.value
    .filter(p => p.active)
    .reduce((sum, p) => sum + p.capacityGb, 0);
});

const loadData = async () => {
  try {
    const [hostRes, vmsRes, storageRes] = await Promise.all([
      getHostInfo(),
      getVms(),
      getStoragePools(),
    ]);
    hostInfo.value = hostRes;
    vms.value = vmsRes;
    storagePools.value = storageRes;
  } catch (error) {
    console.error('加载监控指标失败', error);
  }
};

const renderMemoryChart = () => {
  if (!memoryChartRef.value) return;
  if (!memoryChart) {
    memoryChart = echarts.init(memoryChartRef.value);
  }
  const info = hostInfo.value;
  const used = info ? info.usedMemoryMb : 0;
  const free = info ? info.freeMemoryMb : 1;
  const percent = info ? info.memoryUsagePercent : 0;

  memoryChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      formatter: '{b}: {c} MB ({d}%)',
      backgroundColor: '#ffffff',
      borderColor: '#e4dec9',
      borderWidth: 1,
      textStyle: { color: '#2c2520' }
    },
    series: [
      {
        name: '内存使用',
        type: 'pie',
        radius: ['60%', '80%'],
        avoidLabelOverlap: false,
        itemStyle: {
          borderRadius: 6,
          borderColor: '#ffffff',
          borderWidth: 2
        },
        label: {
          show: true,
          position: 'center',
          formatter: `${percent}%\n已用`,
          fontSize: 18,
          fontWeight: 'bold',
          color: '#2c2520',
          lineHeight: 24
        },
        data: [
          { value: used, name: '已用内存', itemStyle: { color: '#ca6a1f' } },
          { value: free, name: '空闲内存', itemStyle: { color: '#f2ede0' } }
        ]
      }
    ]
  });
};

const renderStatusChart = () => {
  if (!statusChartRef.value) return;
  if (!statusChart) {
    statusChart = echarts.init(statusChartRef.value);
  }

  const stateCounts: Record<string, number> = {};
  vms.value.forEach(vm => {
    const s = vm.state;
    stateCounts[s] = (stateCounts[s] || 0) + 1;
  });

  const rawData = [
    { name: '运行中', key: '运行', color: '#15803d' },
    { name: '已关机', key: '关闭', color: '#746c63' },
    { name: '暂停中', key: '暂停', color: '#b45309' },
    { name: '阻断中', key: '异常', color: '#b91c1c' }
  ];

  const chartData = rawData.map(item => ({
    name: item.name,
    value: stateCounts[item.key] || 0,
    itemStyle: { color: item.color }
  })).filter(item => item.value > 0);

  if (chartData.length === 0) {
    chartData.push({ name: '无实例', value: 0, itemStyle: { color: '#f2ede0' } });
  }

  statusChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: '#ffffff',
      borderColor: '#e4dec9',
      borderWidth: 1,
      textStyle: { color: '#2c2520' }
    },
    legend: {
      bottom: '0%',
      left: 'center',
      textStyle: { color: '#746c63' },
      itemWidth: 10,
      itemHeight: 10
    },
    series: [
      {
        name: '状态统计',
        type: 'pie',
        radius: '55%',
        center: ['50%', '45%'],
        data: chartData,
        emphasis: {
          itemStyle: {
            shadowBlur: 10,
            shadowOffsetX: 0,
            shadowColor: 'rgba(44, 37, 32, 0.1)'
          }
        },
        label: {
          show: true,
          color: '#2c2520',
          formatter: '{b}: {c}台'
        }
      }
    ]
  });
};

const renderStorageChart = () => {
  if (!storageChartRef.value) return;
  if (!storageChart) {
    storageChart = echarts.init(storageChartRef.value);
  }

  const activePools = storagePools.value.filter(p => p.active);
  const poolNames = activePools.map(p => p.name);
  const allocated = activePools.map(p => p.allocationGb);
  const available = activePools.map(p => p.availableGb);

  if (poolNames.length === 0) {
    poolNames.push('无活跃存储池');
    allocated.push(0);
    available.push(0);
  }

  storageChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: '#ffffff',
      borderColor: '#e4dec9',
      borderWidth: 1,
      textStyle: { color: '#2c2520' }
    },
    legend: {
      bottom: '0%',
      textStyle: { color: '#746c63' }
    },
    grid: {
      top: '10%',
      left: '3%',
      right: '4%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'value',
      axisLabel: { color: '#746c63' },
      splitLine: { lineStyle: { color: 'rgba(228, 222, 201, 0.5)' } }
    },
    yAxis: {
      type: 'category',
      data: poolNames,
      axisLabel: { color: '#746c63' }
    },
    series: [
      {
        name: '已用空间 (GB)',
        type: 'bar',
        stack: 'total',
        label: { show: false },
        emphasis: { focus: 'series' },
        data: allocated,
        itemStyle: { color: '#1d4ed8' }
      },
      {
        name: '剩余空间 (GB)',
        type: 'bar',
        stack: 'total',
        label: { show: false },
        emphasis: { focus: 'series' },
        data: available,
        itemStyle: { color: '#f2ede0' }
      }
    ]
  });
};

const updateCharts = () => {
  renderMemoryChart();
  renderStatusChart();
  renderStorageChart();
};

const resizeCharts = () => {
  memoryChart?.resize();
  statusChart?.resize();
  storageChart?.resize();
};

onMounted(async () => {
  await loadData();
  updateCharts();
  window.addEventListener('resize', resizeCharts);
});

onUnmounted(() => {
  window.removeEventListener('resize', resizeCharts);
  memoryChart?.dispose();
  statusChart?.dispose();
  storageChart?.dispose();
});

watch([hostInfo, vms, storagePools], () => {
  updateCharts();
});
</script>

<style scoped>
.dashboard-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stat-card-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.stat-title {
  font-size: 13px;
  color: var(--text-secondary);
  font-weight: 500;
}

.stat-value {
  font-family: var(--font-title);
  font-size: 28px;
  font-weight: 700;
  color: var(--text-primary);
  margin: 2px 0;
}

.stat-desc {
  font-size: 11px;
  color: var(--text-muted);
}

.text-success {
  color: var(--color-success) !important;
}

.text-info {
  color: var(--color-info) !important;
}

.text-purple {
  color: #a78bfa !important;
}

.chart-row {
  margin-top: 10px;
}

.chart-box {
  height: 260px;
  width: 100%;
}

:deep(.el-card__header) {
  border-bottom: 1px solid var(--border-color) !important;
  font-family: var(--font-title);
  font-size: 14px;
  font-weight: 600;
  color: var(--text-primary);
  padding: 12px 20px !important;
}
</style>
