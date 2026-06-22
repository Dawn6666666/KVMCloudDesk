<template>
  <div class="dashboard-container">
    <!-- Top Stats Cards -->
    <div class="stat-cards-container">
      <el-card shadow="hover" class="stat-card">
        <div class="stat-card-content">
          <span class="stat-title">虚拟机总数</span>
          <span class="stat-value">{{ vms.length }}</span>
          <span class="stat-desc">已注册的虚拟主机实例</span>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-card-content">
          <span class="stat-title">运行中实例</span>
          <span class="stat-value text-success">{{ runningVmsCount }}</span>
          <span class="stat-desc">当前正在运行的虚拟机</span>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-card-content">
          <span class="stat-title">宿主机 CPU 占用</span>
          <span class="stat-value text-warning">{{ hostInfo ? hostInfo.cpuUsagePercent : 0 }}%</span>
          <span class="stat-desc">实时处理器使用率</span>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-card-content">
          <span class="stat-title">物理内存占用</span>
          <span class="stat-value text-purple">{{ hostInfo ? hostInfo.memoryUsagePercent : 0 }}%</span>
          <span class="stat-desc">实时物理内存使用率</span>
        </div>
      </el-card>
      <el-card shadow="hover" class="stat-card">
        <div class="stat-card-content">
          <span class="stat-title">存储池容量</span>
          <span class="stat-value text-info">{{ totalStoragePoolCapacity.toFixed(1) }} GB</span>
          <span class="stat-desc">活跃存储池总计容量</span>
        </div>
      </el-card>
    </div>

    <!-- Real-time Metrics & Allocations Charts -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <el-card header="宿主机物理负载实时趋势 (CPU / 内存走势)" shadow="hover">
          <div ref="trendChartRef" class="chart-box main-chart"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="各虚拟机分配资源对比" shadow="hover">
          <div ref="allocationChartRef" class="chart-box main-chart"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Global Stats Charts -->
    <el-row :gutter="20" class="chart-row small-charts-row">
      <el-col :span="8">
        <el-card header="宿主机物理内存分布" shadow="hover">
          <div ref="memoryChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="云虚拟机状态统计" shadow="hover">
          <div ref="statusChartRef" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card header="物理存储池容量分配" shadow="hover">
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

// ECharts 元素引用
const trendChartRef = ref<HTMLElement | null>(null);
const allocationChartRef = ref<HTMLElement | null>(null);
const memoryChartRef = ref<HTMLElement | null>(null);
const statusChartRef = ref<HTMLElement | null>(null);
const storageChartRef = ref<HTMLElement | null>(null);

let trendChart: echarts.ECharts | null = null;
let allocationChart: echarts.ECharts | null = null;
let memoryChart: echarts.ECharts | null = null;
let statusChart: echarts.ECharts | null = null;
let storageChart: echarts.ECharts | null = null;

// 实时 CPU / 内存趋势数据历史记录
const cpuHistory = ref<number[]>([]);
const memoryHistory = ref<number[]>([]);
const timelineLabels = ref<string[]>([]);

const runningVmsCount = computed(() => {
  return vms.value.filter(vm => vm.state === '运行').length;
});

const totalStoragePoolCapacity = computed(() => {
  return storagePools.value
    .filter(p => p.active)
    .reduce((sum, p) => sum + p.capacityGb, 0);
});

// 向历史记录时序中推送实时采样指标
const pushMetricsToHistory = () => {
  const timeStr = new Date().toLocaleTimeString('zh-CN', { hour12: false });
  cpuHistory.value.push(hostInfo.value ? hostInfo.value.cpuUsagePercent : 0);
  memoryHistory.value.push(hostInfo.value ? hostInfo.value.memoryUsagePercent : 0);
  timelineLabels.value.push(timeStr);
  
  // 仅在图表中保留最近 10 个数据采样点（30秒）
  if (cpuHistory.value.length > 10) {
    cpuHistory.value.shift();
    memoryHistory.value.shift();
    timelineLabels.value.shift();
  }
};

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
    pushMetricsToHistory();
  } catch (error) {
    console.error('加载监控指标失败', error);
  }
};

// 渲染大图表：实时 CPU 与内存负载折线图
const renderTrendChart = () => {
  if (!trendChartRef.value) return;
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value);
  }
  trendChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: '#ffffff',
      borderColor: '#e4dec9',
      borderWidth: 1,
      textStyle: { color: '#2c2520' }
    },
    legend: {
      data: ['CPU 利用率', '内存利用率'],
      textStyle: { color: '#746c63' },
      top: '0%'
    },
    grid: {
      top: '15%',
      left: '3%',
      right: '4%',
      bottom: '3%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: timelineLabels.value,
      axisLabel: { color: '#746c63' }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: {
        formatter: '{value}%',
        color: '#746c63'
      },
      splitLine: { lineStyle: { color: 'rgba(228, 222, 201, 0.3)' } }
    },
    series: [
      {
        name: 'CPU 利用率',
        type: 'line',
        data: cpuHistory.value,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 3, color: '#ca6a1f' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(202, 106, 31, 0.3)' },
            { offset: 1, color: 'rgba(202, 106, 31, 0.0)' }
          ])
        },
        itemStyle: { color: '#ca6a1f' }
      },
      {
        name: '内存利用率',
        type: 'line',
        data: memoryHistory.value,
        smooth: true,
        showSymbol: false,
        lineStyle: { width: 3, color: '#1d4ed8' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(29, 78, 216, 0.3)' },
            { offset: 1, color: 'rgba(29, 78, 216, 0.0)' }
          ])
        },
        itemStyle: { color: '#1d4ed8' }
      }
    ]
  });
};

// 渲染大图表：虚拟机 CPU / 内存配额占比柱状图
const renderAllocationChart = () => {
  if (!allocationChartRef.value) return;
  if (!allocationChart) {
    allocationChart = echarts.init(allocationChartRef.value);
  }
  
  const vmNames = vms.value.map(vm => vm.name);
  const cpus = vms.value.map(vm => vm.cpuCount);
  const memories = vms.value.map(vm => vm.memoryMb);
  
  if (vmNames.length === 0) {
    vmNames.push('暂无虚拟机');
    cpus.push(0);
    memories.push(0);
  }
  
  allocationChart.setOption({
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
      data: ['分配 CPU (核)', '分配内存 (MB)'],
      textStyle: { color: '#746c63' },
      bottom: '0%'
    },
    grid: {
      top: '12%',
      left: '3%',
      right: '3%',
      bottom: '15%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      data: vmNames,
      axisLabel: { color: '#746c63', rotate: 20 }
    },
    yAxis: [
      {
        type: 'value',
        name: 'CPU',
        min: 0,
        axisLabel: { color: '#746c63', formatter: '{value} 核' },
        splitLine: { show: false }
      },
      {
        type: 'value',
        name: '内存',
        min: 0,
        axisLabel: { color: '#746c63', formatter: '{value} MB' },
        splitLine: { lineStyle: { color: 'rgba(228, 222, 201, 0.3)' } }
      }
    ],
    series: [
      {
        name: '分配 CPU (核)',
        type: 'bar',
        data: cpus,
        itemStyle: { color: '#ca6a1f' }
      },
      {
        name: '分配内存 (MB)',
        type: 'bar',
        yAxisIndex: 1,
        data: memories,
        itemStyle: { color: '#15803d' }
      }
    ]
  });
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
  renderTrendChart();
  renderAllocationChart();
  renderMemoryChart();
  renderStatusChart();
  renderStorageChart();
};

const resizeCharts = () => {
  trendChart?.resize();
  allocationChart?.resize();
  memoryChart?.resize();
  statusChart?.resize();
  storageChart?.resize();
};

let timer: any = null;

onMounted(async () => {
  await loadData();
  updateCharts();
  window.addEventListener('resize', resizeCharts);
  
  // 每 3 秒自动轮询更新最新系统及虚拟机指标
  timer = setInterval(async () => {
    try {
      const [hostRes, vmsRes, storageRes] = await Promise.all([
        getHostInfo(),
        getVms(),
        getStoragePools(),
      ]);
      hostInfo.value = hostRes;
      vms.value = vmsRes;
      storagePools.value = storageRes;
      pushMetricsToHistory();
    } catch (error) {
      console.error('定时刷新监控指标失败', error);
    }
  }, 3000);
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
    timer = null;
  }
  window.removeEventListener('resize', resizeCharts);
  trendChart?.dispose();
  allocationChart?.dispose();
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

.stat-cards-container {
  display: flex;
  gap: 20px;
  width: 100%;
  flex-wrap: wrap;
}

.stat-card {
  flex: 1;
  min-width: 200px;
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

.text-warning {
  color: var(--color-warning) !important;
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
  height: 250px;
  width: 100%;
}

.main-chart {
  height: 310px;
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
