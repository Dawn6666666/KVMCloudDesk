<template>
  <div class="vms-container">
    <el-card class="vms-card">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>虚拟机实例列表</span>
          </div>
          <div class="header-right">
            <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建虚拟机</el-button>
            <el-button :icon="Refresh" @click="fetchData" :loading="globalLoading">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="globalLoading" :data="vms" style="width: 100%">
        <el-table-column prop="name" label="名称" width="120" fixed />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <div class="state-cell">
              <span :class="['status-dot', getStatusClass(row.state)]"></span>
              <span>{{ translateState(row.state) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="cpuCount" label="CPU (核)" width="90" align="center" />
        <el-table-column label="内存 (MB)" width="100" align="center">
          <template #default="{ row }">
            {{ row.memoryMb }}
          </template>
        </el-table-column>
        <el-table-column label="磁盘规格" width="160">
          <template #default="{ row }">
            <div class="disk-info">
              <span>{{ row.diskSizeGb }} GB</span>
              <span class="sub-text">{{ getFileName(row.diskPath) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="networkName" label="网络" width="110" />
        <el-table-column prop="ipAddress" label="IP 地址" width="130">
          <template #default="{ row }">
            <span>{{ row.ipAddress || '无或未分配' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="自动启动" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.autostart ? 'success' : 'info'" size="small">
              {{ row.autostart ? '开启' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <div class="actions-cell">
              <el-button 
                v-if="row.state === '关闭'" 
                size="small" 
                type="success" 
                plain
                :loading="actionLoading[row.name]"
                @click="handleAction(row.name, 'start')"
              >
                启动
              </el-button>
              <el-button 
                v-if="row.state === '运行'" 
                size="small" 
                type="warning" 
                plain
                :loading="actionLoading[row.name]"
                @click="handleAction(row.name, 'shutdown')"
              >
                关机
              </el-button>
              <el-button 
                v-if="row.state === '运行'" 
                size="small" 
                type="danger" 
                plain
                :loading="actionLoading[row.name]"
                @click="confirmAction(row.name, 'destroy', '强制关机')"
              >
                断电
              </el-button>
              <el-button 
                v-if="row.state === '运行'" 
                size="small" 
                type="info" 
                plain
                :loading="actionLoading[row.name]"
                @click="handleAction(row.name, 'suspend')"
              >
                暂停
              </el-button>
              <el-button 
                v-if="row.state === '暂停'" 
                size="small" 
                type="primary" 
                plain
                :loading="actionLoading[row.name]"
                @click="handleAction(row.name, 'resume')"
              >
                恢复
              </el-button>
              <el-button 
                size="small" 
                type="danger"
                :loading="actionLoading[row.name]"
                @click="confirmAction(row.name, 'delete', '删除虚拟机')"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Create VM Dialog -->
    <el-dialog 
      v-model="createDialogVisible" 
      title="创建新虚拟机实例" 
      width="560px"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="formRef" 
        :model="createForm" 
        :rules="formRules" 
        label-width="100px"
        label-position="left"
      >
        <el-form-item label="主机名称" prop="name">
          <el-input v-model="createForm.name" placeholder="请输入虚拟机名称" />
        </el-form-item>
        <el-form-item label="CPU 核心" prop="cpuCount">
          <el-input-number v-model="createForm.cpuCount" :min="1" :max="16" />
        </el-form-item>
        <el-form-item label="物理内存" prop="memoryMb">
          <el-input-number v-model="createForm.memoryMb" :min="512" :max="32768" :step="512" />
          <span class="unit-text">MB</span>
        </el-form-item>
        <el-form-item label="磁盘空间" prop="diskSizeGb">
          <el-input-number v-model="createForm.diskSizeGb" :min="5" :max="500" />
          <span class="unit-text">GB</span>
        </el-form-item>
        <el-form-item label="安装镜像" prop="imageName">
          <el-select v-model="createForm.imageName" placeholder="请选择系统镜像">
            <el-option 
              v-for="img in images" 
              :key="img.name" 
              :label="img.name" 
              :value="img.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="桥接局域网" prop="networkName">
          <el-select v-model="createForm.networkName" placeholder="请选择虚拟网络">
            <el-option 
              v-for="net in networks" 
              :key="net.name" 
              :label="net.name" 
              :value="net.name"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="详细描述" prop="description">
          <el-input 
            v-model="createForm.description" 
            type="textarea" 
            rows="3" 
            placeholder="关于该虚拟主机的描述" 
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
import { 
  getVms, startVm, shutdownVm, destroyVm, suspendVm, resumeVm, deleteVm, createVm,
  getImages, getNetworks
} from '@/api/kvm';
import type { VmInfoDto, ImageInfoDto, NetworkInfoDto, CreateVmRequest } from '@/types/kvm';

const vms = ref<VmInfoDto[]>([]);
const images = ref<ImageInfoDto[]>([]);
const networks = ref<NetworkInfoDto[]>([]);

const globalLoading = ref(false);
const createLoading = ref(false);
const createDialogVisible = ref(false);
const actionLoading = ref<Record<string, boolean>>({});

const formRef = ref<FormInstance | null>(null);

const createForm = ref<CreateVmRequest>({
  name: '',
  cpuCount: 1,
  memoryMb: 1024,
  diskSizeGb: 10,
  imageName: '',
  networkName: '',
  description: ''
});

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入虚拟机名称', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: '仅能使用英文字母、数字、下划线和连字符', trigger: 'blur' }
  ],
  cpuCount: [{ required: true, message: '请配置处理器核心数', trigger: 'blur' }],
  memoryMb: [{ required: true, message: '请配置内存空间大小', trigger: 'blur' }],
  diskSizeGb: [{ required: true, message: '请设置磁盘大小', trigger: 'blur' }],
  imageName: [{ required: true, message: '请选择基础镜像', trigger: 'change' }],
  networkName: [{ required: true, message: '请选择关联网络', trigger: 'change' }]
};

const translateState = (state: string) => {
  if (state === '运行') return '运行中';
  if (state === '关闭') return '已关机';
  if (state === '暂停') return '暂停中';
  if (state === '异常') return '异常';
  return state;
};

const getStatusClass = (state: string) => {
  if (state === '运行') return 'active';
  if (state === '暂停') return 'paused';
  if (state === '关闭') return 'inactive';
  return 'error';
};

const getFileName = (path: string) => {
  if (!path) return '';
  return path.split(/[/\\]/).pop() || path;
};

const fetchData = async () => {
  globalLoading.value = true;
  try {
    const data = await getVms();
    vms.value = data;
  } catch (error) {
    console.error('获取虚拟机列表异常', error);
  } finally {
    globalLoading.value = false;
  }
};

const fetchResources = async () => {
  try {
    const [imagesData, networksData] = await Promise.all([
      getImages(),
      getNetworks()
    ]);
    images.value = imagesData;
    networks.value = networksData;
  } catch (error) {
    console.error('获取基础组件依赖异常', error);
  }
};

const openCreateDialog = async () => {
  createDialogVisible.value = true;
  createForm.value = {
    name: '',
    cpuCount: 1,
    memoryMb: 1024,
    diskSizeGb: 10,
    imageName: '',
    networkName: '',
    description: ''
  };
  await fetchResources();
};

const submitCreate = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      createLoading.value = true;
      try {
        await createVm(createForm.value);
        ElMessage.success('创建指令发送成功');
        createDialogVisible.value = false;
        fetchData();
      } catch (error) {
        console.error('新建虚拟机失败', error);
      } finally {
        createLoading.value = false;
      }
    }
  });
};

const handleAction = async (name: string, action: string) => {
  actionLoading.value[name] = true;
  try {
    switch (action) {
      case 'start':
        await startVm(name);
        break;
      case 'shutdown':
        await shutdownVm(name);
        break;
      case 'suspend':
        await suspendVm(name);
        break;
      case 'resume':
        await resumeVm(name);
        break;
    }
    ElMessage.success('操作指令成功完成');
    await fetchData();
  } catch (error) {
    console.error(`执行 ${action} 操作异常`, error);
  } finally {
    actionLoading.value[name] = false;
  }
};

const confirmAction = (name: string, action: string, actionTitle: string) => {
  ElMessageBox.confirm(
    `确定要对虚拟机 ${name} 执行 ${actionTitle} 操作吗？该操作可能导致数据未保存或实例被彻底清除。`,
    '安全警告提示',
    {
      confirmButtonText: '确定执行',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    actionLoading.value[name] = true;
    try {
      if (action === 'destroy') {
        await destroyVm(name);
      } else if (action === 'delete') {
        await deleteVm(name);
      }
      ElMessage.success(`${actionTitle}指令执行完成`);
      await fetchData();
    } catch (error) {
      console.error(`执行 ${actionTitle} 操作发生错误`, error);
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
.vms-container {
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-right {
  display: flex;
  gap: 12px;
}

.state-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.disk-info {
  display: flex;
  flex-direction: column;
}

.sub-text {
  font-size: 11px;
  color: var(--text-muted);
  word-break: break-all;
}

.actions-cell {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.unit-text {
  margin-left: 10px;
  color: var(--text-secondary);
  font-size: 13px;
}

:deep(.el-input-number) {
  width: 140px;
}

:deep(.el-select) {
  width: 100%;
}
</style>
