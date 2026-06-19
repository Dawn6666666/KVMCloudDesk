<template>
  <div class="networks-container">
    <el-card class="networks-card">
      <template #header>
        <div class="card-header">
          <span>虚拟网络配置与管理</span>
          <el-button :icon="Refresh" @click="fetchData" :loading="globalLoading">刷新</el-button>
        </div>
      </template>

      <el-table v-loading="globalLoading" :data="networks" style="width: 100%">
        <el-table-column prop="name" label="网络名称" width="130" fixed />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <div class="state-cell">
              <span :class="['status-dot', row.active ? 'active' : 'inactive']"></span>
              <span>{{ row.active ? '活动' : '不活动' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="自动启动" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.autostart ? 'success' : 'info'" size="small">
              {{ row.autostart ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="bridgeName" label="桥接网卡名称" width="130" />
        <el-table-column prop="forwardMode" label="转发模式" width="100" align="center">
          <template #default="{ row }">
            <el-tag type="warning" size="small" effect="plain">
              {{ (row.forwardMode || 'NAT').toUpperCase() }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="ipAddress" label="网关 IP" width="130" />
        <el-table-column label="DHCP 分配范围" min-width="220">
          <template #default="{ row }">
            <div v-if="row.dhcpStart && row.dhcpEnd" class="dhcp-range">
              <code>{{ row.dhcpStart }}</code>
              <span class="range-arrow">~</span>
              <code>{{ row.dhcpEnd }}</code>
            </div>
            <span v-else class="text-muted">未启用 DHCP</span>
          </template>
        </el-table-column>
        <el-table-column prop="uuid" label="UUID" min-width="240" show-overflow-tooltip />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <div class="actions-cell">
              <el-button 
                v-if="!row.active" 
                size="small" 
                type="success" 
                plain
                :loading="actionLoading[row.name]"
                @click="handleAction(row.name, 'start')"
              >
                启动
              </el-button>
              <el-button 
                v-if="row.active" 
                size="small" 
                type="danger" 
                plain
                :loading="actionLoading[row.name]"
                @click="confirmStop(row.name)"
              >
                停止
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { getNetworks, startNetwork, stopNetwork } from '@/api/kvm';
import type { NetworkInfoDto } from '@/types/kvm';

const networks = ref<NetworkInfoDto[]>([]);
const globalLoading = ref(false);
const actionLoading = ref<Record<string, boolean>>({});

const fetchData = async () => {
  globalLoading.value = true;
  try {
    const data = await getNetworks();
    networks.value = data;
  } catch (error) {
    console.error('加载虚拟网络列表异常', error);
  } finally {
    globalLoading.value = false;
  }
};

const handleAction = async (name: string, action: string) => {
  actionLoading.value[name] = true;
  try {
    if (action === 'start') {
      await startNetwork(name);
      ElMessage.success('虚拟网络启动成功');
    }
    await fetchData();
  } catch (error) {
    console.error(`执行网络 ${action} 异常`, error);
  } finally {
    actionLoading.value[name] = false;
  }
};

const confirmStop = (name: string) => {
  ElMessageBox.confirm(
    `确定要停止虚拟网络 ${name} 吗？停止网络会导致所有连接至此网络的虚拟机无法进行外部网络通信。`,
    '网络停止警告',
    {
      confirmButtonText: '确定停止',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    actionLoading.value[name] = true;
    try {
      await stopNetwork(name);
      ElMessage.success('虚拟网络停止成功');
      await fetchData();
    } catch (error) {
      console.error('停止虚拟网络异常', error);
    } finally {
      actionLoading.value[name] = false;
    }
  }).catch(() => {});
};

onMounted(() => {
  fetchData();
});
</script>

<style scoped>
.networks-container {
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.state-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.dhcp-range {
  display: flex;
  align-items: center;
  gap: 6px;
}

.dhcp-range code {
  font-family: 'Consolas', monospace;
  background-color: rgba(255, 255, 255, 0.05);
  padding: 1px 6px;
  border-radius: 4px;
}

.range-arrow {
  color: var(--text-muted);
}

.actions-cell {
  display: flex;
  gap: 6px;
}
</style>
