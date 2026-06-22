<template>
  <div class="snapshots-container">
    <el-card class="snapshots-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span class="title-text">虚拟机快照恢复与管理</span>
            <div class="vm-selector-box">
              <span class="label">选择虚拟机:</span>
              <el-select 
                v-model="selectedVmName" 
                placeholder="请选择虚拟机实例" 
                @change="onVmChange"
                style="width: 200px"
              >
                <el-option 
                  v-for="vm in vms" 
                  :key="vm.name" 
                  :label="`${vm.name} (${translateState(vm.state)})`" 
                  :value="vm.name" 
                />
              </el-select>
            </div>
          </div>
          <div class="header-right">
            <el-button 
              type="primary" 
              :icon="Plus" 
              :disabled="!selectedVmName" 
              @click="openCreateDialog"
            >
              新建快照
            </el-button>
            <el-button 
              :icon="Refresh" 
              :disabled="!selectedVmName" 
              @click="fetchSnapshots" 
              :loading="globalLoading"
            >
              刷新
            </el-button>
          </div>
        </div>
      </template>

      <div v-if="!selectedVmName" class="empty-block">
        <el-empty description="请先在上方下拉框中选择要管理快照的虚拟机实例" />
      </div>

      <div v-else>
        <el-table v-loading="globalLoading" :data="snapshots" style="width: 100%">
          <el-table-column prop="name" label="快照名称" width="220" fixed>
            <template #default="{ row }">
              <div class="name-cell" style="display: flex; align-items: center; gap: 8px;">
                <span>{{ row.name }}</span>
                <el-tag v-if="row.current" size="small" type="success" effect="dark" class="current-badge">当前</el-tag>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="state" label="状态" width="120" align="center">
            <template #default="{ row }">
              <el-tag size="small" :type="getSnapshotStateTagType(row.state)">
                {{ translateSnapshotState(row.state) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建生成时间" width="180" />
          <el-table-column prop="description" label="说明描述" min-width="200" show-overflow-tooltip />
          <el-table-column label="操作" width="200" fixed="right">
            <template #default="{ row }">
              <div class="actions-cell">
                <el-button 
                  size="small" 
                  type="warning" 
                  plain
                  :loading="actionLoading[row.name]"
                  @click="confirmRevert(row.name)"
                >
                  恢复
                </el-button>
                <el-button 
                  size="small" 
                  type="danger" 
                  plain
                  :loading="actionLoading[row.name]"
                  @click="confirmDelete(row.name)"
                >
                  删除
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </el-card>

    <!-- Create Snapshot Dialog -->
    <el-dialog 
      v-model="createDialogVisible" 
      title="新建虚拟机快照" 
      width="480px"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="formRef" 
        :model="createForm" 
        :rules="formRules" 
        label-width="80px"
        label-position="left"
      >
        <el-form-item label="快照名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入快照别名，例如: snap_v1" />
        </el-form-item>
        <el-form-item label="详细说明" prop="description">
          <el-input 
            v-model="createForm.description" 
            type="textarea" 
            rows="3" 
            placeholder="快照时的系统配置或运行环境说明" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="createLoading" @click="submitCreate">确认创建</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Plus, Refresh } from '@element-plus/icons-vue';
import { getVms, getSnapshots, createSnapshot, revertSnapshot, deleteSnapshot } from '@/api/kvm';
import type { VmInfoDto, SnapshotInfoDto, CreateSnapshotRequest } from '@/types/kvm';

const vms = ref<VmInfoDto[]>([]);
const snapshots = ref<SnapshotInfoDto[]>([]);
const selectedVmName = ref<string>('');

const globalLoading = ref(false);
const createLoading = ref(false);
const createDialogVisible = ref(false);
const actionLoading = ref<Record<string, boolean>>({});

const formRef = ref<FormInstance | null>(null);

const createForm = ref<CreateSnapshotRequest>({
  name: '',
  description: ''
});

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入快照名称', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: '仅允许使用英文字母、数字和下划线连字符', trigger: 'blur' }
  ]
};

const translateState = (state: string) => {
  if (state === '运行') return '运行中';
  if (state === '关闭') return '已关机';
  if (state === '暂停') return '暂停中';
  if (state === '异常') return '异常';
  return state;
};

const translateSnapshotState = (state: string) => {
  if (!state) return '未知';
  const lower = state.toLowerCase();
  if (lower === 'running') return '运行中';
  if (lower === 'shutoff') return '已关机';
  if (lower === 'paused') return '暂停中';
  return state;
};

const getSnapshotStateTagType = (state: string) => {
  if (!state) return 'info';
  const lower = state.toLowerCase();
  if (lower === 'running') return 'success';
  if (lower === 'shutoff') return 'info';
  if (lower === 'paused') return 'warning';
  return 'danger';
};

const fetchVms = async () => {
  try {
    const data = await getVms();
    vms.value = data;
    // 默认选中第一个虚拟机并自动加载快照列表
    if (data && data.length > 0 && !selectedVmName.value) {
      selectedVmName.value = data[0].name;
      fetchSnapshots();
    }
  } catch (error) {
    console.error('获取虚拟机列表失败', error);
  }
};

const fetchSnapshots = async () => {
  if (!selectedVmName.value) return;
  globalLoading.value = true;
  try {
    const data = await getSnapshots(selectedVmName.value);
    snapshots.value = data;
  } catch (error) {
    console.error('获取虚拟机快照列表异常', error);
  } finally {
    globalLoading.value = false;
  }
};

const onVmChange = () => {
  snapshots.value = [];
  fetchSnapshots();
};

const openCreateDialog = () => {
  createDialogVisible.value = true;
  createForm.value = {
    name: '',
    description: ''
  };
};

const submitCreate = async () => {
  if (!formRef.value || !selectedVmName.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      createLoading.value = true;
      try {
        await createSnapshot(selectedVmName.value, createForm.value);
        ElMessage.success('快照创建成功');
        createDialogVisible.value = false;
        fetchSnapshots();
      } catch (error) {
        console.error('创建快照失败', error);
      } finally {
        createLoading.value = false;
      }
    }
  });
};

const confirmRevert = (snapshotName: string) => {
  ElMessageBox.confirm(
    `确定要将虚拟机 ${selectedVmName.value} 恢复到快照 ${snapshotName} 状态吗？这会导致虚拟机当前的所有未保存修改彻底丢失！`,
    '快照恢复安全提示',
    {
      confirmButtonText: '确定回滚',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    actionLoading.value[snapshotName] = true;
    try {
      await revertSnapshot(selectedVmName.value, snapshotName);
      ElMessage.success('系统回滚成功');
      await fetchSnapshots();
    } catch (error) {
      console.error('回滚快照异常', error);
    } finally {
      actionLoading.value[snapshotName] = false;
    }
  }).catch(() => {});
};

const confirmDelete = (snapshotName: string) => {
  ElMessageBox.confirm(
    `确定要永久删除虚拟机 ${selectedVmName.value} 的快照 ${snapshotName} 吗？该操作不可撤销！`,
    '快照删除警告',
    {
      confirmButtonText: '确定删除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    actionLoading.value[snapshotName] = true;
    try {
      await deleteSnapshot(selectedVmName.value, snapshotName);
      ElMessage.success('快照删除成功');
      await fetchSnapshots();
    } catch (error) {
      console.error('删除快照异常', error);
    } finally {
      actionLoading.value[snapshotName] = false;
    }
  }).catch(() => {});
};

onMounted(() => {
  fetchVms();
});
</script>

<style scoped>
.snapshots-container {
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.title-text {
  font-family: var(--font-title);
  font-weight: 600;
}

.vm-selector-box {
  display: flex;
  align-items: center;
  gap: 8px;
}

.vm-selector-box .label {
  font-size: 13px;
  color: var(--text-secondary);
}

.header-right {
  display: flex;
  gap: 12px;
}

.empty-block {
  padding: 40px 0;
}

.actions-cell {
  display: flex;
  gap: 6px;
}
</style>
