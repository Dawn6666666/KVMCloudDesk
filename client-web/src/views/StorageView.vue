<template>
  <div class="storage-container">
    <el-row :gutter="20">
      <!-- Master: Storage Pools -->
      <el-col :span="12">
        <el-card class="pools-card">
          <template #header>
            <div class="card-header">
              <span>物理存储池列表</span>
              <el-button :icon="Refresh" @click="fetchPools" :loading="poolsLoading">刷新</el-button>
            </div>
          </template>

          <el-table 
            v-loading="poolsLoading" 
            :data="pools" 
            highlight-current-row
            @current-change="onPoolSelect"
            style="width: 100%"
          >
            <el-table-column prop="name" label="存储池名称" width="120" fixed />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <div class="state-cell">
                  <span :class="['status-dot', row.active ? 'active' : 'inactive']"></span>
                  <span>{{ row.active ? '活动' : '下线' }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="空间分配率" min-width="150">
              <template #default="{ row }">
                <div v-if="row.active" class="usage-progress">
                  <el-progress 
                    :percentage="Number(((row.allocationGb / row.capacityGb) * 100).toFixed(1))" 
                    :status="getProgressStatus((row.allocationGb / row.capacityGb) * 100)"
                    :stroke-width="12"
                    text-inside
                  />
                  <span class="usage-text">
                    已用 {{ row.allocationGb.toFixed(1) }}G / 共 {{ row.capacityGb.toFixed(1) }}G
                  </span>
                </div>
                <span v-else class="text-muted">不可用</span>
              </template>
            </el-table-column>
            <el-table-column prop="path" label="挂载目录" min-width="150" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>

      <!-- Detail: Storage Volumes -->
      <el-col :span="12">
        <el-card class="volumes-card">
          <template #header>
            <div class="card-header">
              <span>
                {{ selectedPoolName ? `存储卷列表 - ${selectedPoolName}` : '存储卷列表 (请先选择左侧存储池)' }}
              </span>
              <el-button 
                v-if="selectedPoolName" 
                :icon="Refresh" 
                @click="fetchVolumes" 
                :loading="volumesLoading"
              >
                刷新
              </el-button>
            </div>
          </template>

          <div v-if="!selectedPoolName" class="empty-block">
            <el-empty description="请在左侧点击选择一个存储池以查看其具体的磁盘映像卷" />
          </div>

          <div v-else>
            <el-table v-loading="volumesLoading" :data="volumes" style="width: 100%">
              <el-table-column prop="name" label="卷名称" min-width="130" fixed />
              <el-table-column prop="type" label="格式类型" width="90" align="center">
                <template #default="{ row }">
                  <el-tag size="small" type="success" effect="plain">
                    {{ (row.type || 'file').toUpperCase() }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="分配 / 预设容量" min-width="140">
                <template #default="{ row }">
                  {{ row.allocationGb.toFixed(1) }} GB / {{ row.capacityGb.toFixed(1) }} GB
                </template>
              </el-table-column>
              <el-table-column prop="path" label="卷文件路径" min-width="160" show-overflow-tooltip />
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Refresh } from '@element-plus/icons-vue';
import { getStoragePools, getStoragePoolVolumes } from '@/api/kvm';
import type { StoragePoolInfoDto, StorageVolumeInfoDto } from '@/types/kvm';

const pools = ref<StoragePoolInfoDto[]>([]);
const volumes = ref<StorageVolumeInfoDto[]>([]);
const selectedPoolName = ref<string>('');

const poolsLoading = ref(false);
const volumesLoading = ref(false);

const getProgressStatus = (percentage: number) => {
  if (percentage > 90) return 'exception';
  if (percentage > 70) return 'warning';
  return 'success';
};

const fetchPools = async () => {
  poolsLoading.value = true;
  try {
    const data = await getStoragePools();
    pools.value = data;
    // Clear selection if current selection is not in the active pools anymore
    if (selectedPoolName.value && !data.some(p => p.name === selectedPoolName.value && p.active)) {
      selectedPoolName.value = '';
      volumes.value = [];
    }
  } catch (error) {
    console.error('加载存储池列表异常', error);
  } finally {
    poolsLoading.value = false;
  }
};

const fetchVolumes = async () => {
  if (!selectedPoolName.value) return;
  volumesLoading.value = true;
  try {
    const data = await getStoragePoolVolumes(selectedPoolName.value);
    volumes.value = data;
  } catch (error) {
    console.error('加载磁盘卷列表异常', error);
  } finally {
    volumesLoading.value = false;
  }
};

const onPoolSelect = (row: StoragePoolInfoDto | null) => {
  if (row && row.active) {
    selectedPoolName.value = row.name;
    fetchVolumes();
  } else {
    selectedPoolName.value = '';
    volumes.value = [];
  }
};

onMounted(() => {
  fetchPools();
});
</script>

<style scoped>
.storage-container {
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

.usage-progress {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.usage-text {
  font-size: 11px;
  color: var(--text-muted);
}

.empty-block {
  padding: 40px 0;
}
</style>
