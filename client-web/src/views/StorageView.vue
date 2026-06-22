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
            ref="poolsTableRef"
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
                {{ selectedPoolName ? `存储卷列表 - ${selectedPoolName}` : '存储卷列表 - 请先选择左侧存储池' }}
              </span>
              <div v-if="selectedPoolName" class="header-actions">
                <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">新建卷</el-button>
                <el-button :icon="Refresh" @click="fetchVolumes" :loading="volumesLoading">刷新</el-button>
              </div>
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
              <el-table-column label="关联虚拟机" min-width="110" align="center">
                <template #default="{ row }">
                  <el-link v-if="row.vmName" type="warning" @click="goToVm(row.vmName)">{{ row.vmName }}</el-link>
                  <el-tag v-else type="info" size="small" effect="plain">闲置</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" align="center" fixed="right">
                <template #default="{ row }">
                  <el-button 
                    link 
                    type="danger" 
                    :disabled="!!row.vmName" 
                    @click="handleDeleteVolume(row.name)"
                  >
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新建卷弹窗 -->
    <el-dialog 
      v-model="showCreateDialog" 
      title="新建存储卷" 
      width="460px"
      @close="resetForm"
    >
      <el-form 
        ref="formRef" 
        :model="form" 
        :rules="rules" 
        label-width="100px" 
        label-position="right"
      >
        <el-form-item label="卷名称" prop="name">
          <el-input v-model="form.name" placeholder="例如: new-disk.qcow2" />
        </el-form-item>
        <el-form-item label="容量" prop="capacityGb">
          <el-input-number 
            v-model="form.capacityGb" 
            :min="1" 
            :max="1000" 
            :precision="1" 
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="格式类型" prop="format">
          <el-select v-model="form.format" style="width: 100%">
            <el-option label="QCOW2" value="qcow2" />
            <el-option label="RAW" value="raw" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" :loading="submitLoading" @click="submitForm">确定</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Refresh, Plus } from '@element-plus/icons-vue';
import { getStoragePools, getStoragePoolVolumes, createStorageVolume, deleteStorageVolume } from '@/api/kvm';
import type { StoragePoolInfoDto, StorageVolumeInfoDto } from '@/types/kvm';
import { ElMessage, ElMessageBox } from 'element-plus';
import type { FormInstance } from 'element-plus';

const pools = ref<StoragePoolInfoDto[]>([]);
const volumes = ref<StorageVolumeInfoDto[]>([]);
const selectedPoolName = ref<string>('');
const poolsTableRef = ref<any>(null);

const router = useRouter();
const goToVm = (name: string) => {
  router.push({ path: '/vms', query: { search: name } });
};

const poolsLoading = ref(false);
const volumesLoading = ref(false);

const showCreateDialog = ref(false);
const submitLoading = ref(false);
const formRef = ref<FormInstance>();
const form = ref({
  name: '',
  capacityGb: 10,
  format: 'qcow2'
});

const rules = {
  name: [
    { required: true, message: '请输入卷名称', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_\-\.]+$/, message: '名称只能包含英文字母、数字、下划线、中划线和点', trigger: 'blur' }
  ],
  capacityGb: [
    { required: true, message: '请输入容量', trigger: 'blur' }
  ],
  format: [
    { required: true, message: '请选择格式类型', trigger: 'blur' }
  ]
};

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
    
    // 如果之前选中的存储池已经不在活动的存储池中了，清空它
    if (selectedPoolName.value && !data.some(p => p.name === selectedPoolName.value && p.active)) {
      selectedPoolName.value = '';
      volumes.value = [];
    }
    
    // 默认选中第一个活跃的存储池
    if (data && data.length > 0) {
      const currentActive = data.find(p => p.name === selectedPoolName.value && p.active);
      if (currentActive) {
        setTimeout(() => {
          poolsTableRef.value?.setCurrentRow(currentActive);
        }, 50);
      } else {
        const defaultActive = data.find(p => p.active);
        if (defaultActive) {
          selectedPoolName.value = defaultActive.name;
          fetchVolumes();
          setTimeout(() => {
            poolsTableRef.value?.setCurrentRow(defaultActive);
          }, 50);
        }
      }
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

const resetForm = () => {
  if (formRef.value) {
    formRef.value.resetFields();
  }
  form.value = {
    name: '',
    capacityGb: 10,
    format: 'qcow2'
  };
};

const submitForm = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true;
      try {
        await createStorageVolume(selectedPoolName.value, form.value);
        ElMessage.success('创建存储卷成功');
        showCreateDialog.value = false;
        fetchVolumes();
        fetchPools();
      } catch (error: any) {
        ElMessage.error(error.message || '创建存储卷失败');
      } finally {
        submitLoading.value = false;
      }
    }
  });
};

const handleDeleteVolume = (volumeName: string) => {
  ElMessageBox.confirm(
    `确定要永久删除存储卷 ${volumeName} 吗？此操作不可逆。`,
    '警告',
    {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    try {
      await deleteStorageVolume(selectedPoolName.value, volumeName);
      ElMessage.success('删除存储卷成功');
      fetchVolumes();
      fetchPools();
    } catch (error: any) {
      ElMessage.error(error.message || '删除存储卷失败');
    }
  }).catch(() => {});
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

.header-actions {
  display: flex;
  gap: 8px;
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
