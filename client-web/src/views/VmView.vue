<template>
  <div class="vms-container">
    <el-card class="vms-card">
      <template #header>
        <div class="card-header">
          <div class="header-left" style="display: flex; align-items: center; gap: 16px;">
            <span>虚拟机实例列表</span>
            <el-input
              v-model="searchQuery"
              placeholder="输入虚拟机名称过滤..."
              clearable
              :prefix-icon="Search"
              style="width: 220px"
            />
            <el-tag 
              v-if="route.query.network" 
              closable 
              type="warning"
              @click="clearNetworkFilter"
              @close="clearNetworkFilter"
              style="cursor: pointer"
            >
              网络: {{ route.query.network }}
            </el-tag>
          </div>
          <div class="header-right">
            <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建虚拟机</el-button>
            <el-button :icon="Refresh" @click="fetchData" :loading="globalLoading">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="globalLoading" :data="filteredVms" style="width: 100%">
        <el-table-column prop="name" label="名称" width="120" fixed />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <div class="state-cell">
              <span :class="['status-dot', getStatusClass(row.state)]"></span>
              <span>{{ translateState(row.state) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="cpuCount" label="CPU/核" width="90" align="center" />
        <el-table-column label="内存/MB" width="100" align="center">
          <template #default="{ row }">
            {{ row.memoryMb }}
          </template>
        </el-table-column>
        <el-table-column label="磁盘规格" width="220">
          <template #default="{ row }">
            <div class="disk-info">
              <span style="font-weight: bold;">{{ row.diskSizeGb }} GB</span>
              <div v-for="(path, index) in (row.diskPath ? row.diskPath.split(';') : [])" :key="index" class="sub-text" :title="path">
                磁盘 {{ index + 1 }}: {{ getFileName(path) }}
              </div>
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
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <div class="actions-cell">
              <el-button 
                v-if="row.state === '运行'" 
                size="small" 
                type="primary" 
                plain
                @click="openVncConsole(row.name)"
              >
                控制台
              </el-button>
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
                type="warning" 
                plain
                :loading="actionLoading[row.name]"
                @click="handleAction(row.name, 'reboot')"
              >
                重启
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

    <!-- 虚拟机关闭进度 Dialog -->
    <el-dialog 
      v-model="shutdownDialogVisible" 
      title="虚拟机安全关机控制" 
      width="460px"
      :close-on-click-modal="false"
      :show-close="shutdownProgressStatus === 'exception' || shutdownProgressStatus === 'success'"
      @closed="handleShutdownDialogClosed"
    >
      <div class="shutdown-progress-container" style="text-align: center; padding: 20px 0;">
        <el-progress 
          type="circle" 
          :percentage="shutdownProgress" 
          :status="shutdownProgressStatus" 
          :stroke-width="8" 
          :width="120"
        />
        <div class="progress-status-title" style="margin-top: 20px; font-weight: bold; font-size: 16px; color: var(--el-text-color-primary);">
          正在关闭 {{ shutdownVmName }}
        </div>
        <p class="progress-status-desc" style="margin-top: 10px; font-size: 14px; color: var(--el-text-color-secondary); line-height: 1.5; padding: 0 10px;">
          {{ shutdownStatusText }}
        </p>
      </div>
      <template #footer>
        <div class="dialog-footer" style="display: flex; justify-content: flex-end; gap: 10px;">
          <el-button 
            v-if="shutdownProgressStatus === 'exception' || shutdownProgressStatus === 'success'" 
            @click="shutdownDialogVisible = false"
          >
            关闭窗口
          </el-button>
          <el-button 
            v-if="showForceShutdownBtn" 
            type="danger" 
            :loading="forceShutdownLoading" 
            @click="handleForceShutdown"
          >
            强制断电
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox, ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Plus, Refresh, Search } from '@element-plus/icons-vue';
import { 
  getVms, startVm, shutdownVm, destroyVm, suspendVm, resumeVm, deleteVm, createVm,
  getImages, getNetworks, getVmDetail, rebootVm
} from '@/api/kvm';
import type { VmInfoDto, ImageInfoDto, NetworkInfoDto, CreateVmRequest } from '@/types/kvm';
import { useLogStore } from '@/stores/logStore';

const vms = ref<VmInfoDto[]>([]);
const images = ref<ImageInfoDto[]>([]);
const networks = ref<NetworkInfoDto[]>([]);

const route = useRoute();
const router = useRouter();
const searchQuery = ref('');

const filteredVms = computed(() => {
  let result = vms.value;
  if (route.query.network) {
    result = result.filter(vm => vm.networkName === route.query.network);
  }
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase().trim();
    result = result.filter(vm => vm.name.toLowerCase().includes(q));
  }
  return result;
});

const clearNetworkFilter = () => {
  router.replace({ path: '/vms', query: { ...route.query, network: undefined } });
};

const globalLoading = ref(false);
const createLoading = ref(false);
const createDialogVisible = ref(false);
const actionLoading = ref<Record<string, boolean>>({});

const logStore = useLogStore();

// 关机进度控制
const shutdownDialogVisible = ref(false);
const shutdownVmName = ref('');
const shutdownProgress = ref(0);
const shutdownStatusText = ref('');
const shutdownProgressStatus = ref<'success' | 'exception' | 'warning' | ''>('');
const showForceShutdownBtn = ref(false);
const forceShutdownLoading = ref(false);

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

const openVncConsole = (name: string) => {
  window.open(`/vnc/${name}`, '_blank');
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

let activePollInterval: any = null;
let activeProgressInterval: any = null;

const handleShutdownDialogClosed = () => {
  if (activePollInterval) {
    clearInterval(activePollInterval);
    activePollInterval = null;
  }
  if (activeProgressInterval) {
    clearInterval(activeProgressInterval);
    activeProgressInterval = null;
  }
};

const startShutdownPoll = (name: string) => {
  // 启动前先清理可能残留的定时器
  handleShutdownDialogClosed();

  shutdownVmName.value = name;
  shutdownProgress.value = 10;
  shutdownStatusText.value = '已向系统发送关机信号，正在等待虚拟机关闭...';
  shutdownProgressStatus.value = '';
  showForceShutdownBtn.value = false;
  shutdownDialogVisible.value = true;

  logStore.addLog('info', `关闭虚拟机 ${name}`, '正在等待虚拟机安全关机...');

  let pollCount = 0;
  const maxPoll = 15; // 1.5 秒轮询一次，最多 15 次，共 22.5 秒左右

  activeProgressInterval = setInterval(() => {
    if (shutdownProgress.value < 90) {
      shutdownProgress.value = Math.min(
        90,
        Math.round(shutdownProgress.value + (90 - shutdownProgress.value) * 0.15)
      );
    }
  }, 1000);

  activePollInterval = setInterval(async () => {
    pollCount++;
    try {
      const detail = await getVmDetail(name);
      if (detail.state === '关闭') {
        handleShutdownDialogClosed();

        shutdownProgress.value = 100;
        shutdownProgressStatus.value = 'success';
        shutdownStatusText.value = '虚拟机已成功安全关闭';
        logStore.addLog('success', `关闭虚拟机 ${name}`, '虚拟机已成功安全关机');

        setTimeout(() => {
          shutdownDialogVisible.value = false;
          fetchData();
        }, 1200);
      } else if (pollCount >= maxPoll) {
        handleShutdownDialogClosed();

        shutdownProgressStatus.value = 'exception';
        shutdownStatusText.value = '虚拟机未能在规定时间内响应关机信号。这可能是因为虚拟机内部系统未运行或未安装 ACPI 关机支持。你可以关闭本窗口，或执行强制断电。';
        showForceShutdownBtn.value = true;
        logStore.addLog('error', `关闭虚拟机 ${name}`, '虚拟机关机等待超时');
      }
    } catch (error) {
      console.error('轮询虚拟机状态发生异常', error);
    }
  }, 1500);
};

const handleForceShutdown = async () => {
  forceShutdownLoading.value = true;
  try {
    await destroyVm(shutdownVmName.value);
    ElMessage.success('虚拟机强制关闭成功');
    logStore.addLog('success', `强制关机虚拟机 ${shutdownVmName.value}`, '强制关闭成功');
    shutdownDialogVisible.value = false;
    await fetchData();
  } catch (error) {
    console.error('强制关机失败', error);
    ElMessage.error('强制关机操作失败');
  } finally {
    forceShutdownLoading.value = false;
  }
};

const handleAction = async (name: string, action: string) => {
  actionLoading.value[name] = true;
  const actionName = {
    start: '启动',
    suspend: '暂停',
    resume: '恢复',
    reboot: '重启'
  }[action] || '操作';

  try {
    switch (action) {
      case 'start':
        logStore.addLog('info', `启动虚拟机 ${name}`, '正在向系统发送启动信号...');
        await startVm(name);
        logStore.addLog('info', `启动虚拟机 ${name}`, '启动指令已下发，正在验证虚拟机运行状态...');
        
        await new Promise(resolve => setTimeout(resolve, 600));
        await fetchData();
        
        const vm = vms.value.find(v => v.name === name);
        if (vm && vm.state === '运行') {
          logStore.addLog('success', `启动虚拟机 ${name}`, '虚拟机状态已成功转为 [运行中]');
          ElMessage.success(`虚拟机 ${name} 启动成功`);
        } else {
          logStore.addLog('error', `启动虚拟机 ${name}`, `启动信号已发出，但检测到状态为 [${vm ? vm.state : '未知'}]`);
          ElMessage.warning(`虚拟机 ${name} 已启动，但状态异常`);
        }
        break;

      case 'shutdown':
        await shutdownVm(name);
        startShutdownPoll(name);
        break;

      case 'suspend':
        logStore.addLog('info', `暂停虚拟机 ${name}`, '正在发送暂停信号...');
        await suspendVm(name);
        logStore.addLog('info', `暂停虚拟机 ${name}`, '暂停指令已下发，正在更新状态...');
        
        await new Promise(resolve => setTimeout(resolve, 500));
        await fetchData();
        
        const suspendedVm = vms.value.find(v => v.name === name);
        if (suspendedVm && suspendedVm.state === '暂停') {
          logStore.addLog('success', `暂停虚拟机 ${name}`, '虚拟机已成功进入 [暂停] 状态');
          ElMessage.success(`虚拟机 ${name} 已暂停`);
        } else {
          logStore.addLog('error', `暂停虚拟机 ${name}`, `已发送指令，但当前状态为 [${suspendedVm ? suspendedVm.state : '未知'}]`);
        }
        break;

      case 'resume':
        logStore.addLog('info', `恢复虚拟机 ${name}`, '正在发送恢复信号...');
        await resumeVm(name);
        logStore.addLog('info', `恢复虚拟机 ${name}`, '恢复指令已下发，正在更新状态...');
        
        await new Promise(resolve => setTimeout(resolve, 500));
        await fetchData();
        
        const resumedVm = vms.value.find(v => v.name === name);
        if (resumedVm && resumedVm.state === '运行') {
          logStore.addLog('success', `恢复虚拟机 ${name}`, '虚拟机已成功恢复至 [运行中]');
          ElMessage.success(`虚拟机 ${name} 已恢复运行`);
        } else {
          logStore.addLog('error', `恢复虚拟机 ${name}`, `已发送指令，但当前状态为 [${resumedVm ? resumedVm.state : '未知'}]`);
        }
        break;

      case 'reboot':
        logStore.addLog('info', `重启虚拟机 ${name}`, '正在发送重启信号...');
        await rebootVm(name);
        logStore.addLog('info', `重启虚拟机 ${name}`, '重启指令已下发，正在等待虚拟机重启...');
        
        await new Promise(resolve => setTimeout(resolve, 1000));
        await fetchData();
        
        const rebootedVm = vms.value.find(v => v.name === name);
        if (rebootedVm && rebootedVm.state === '运行') {
          logStore.addLog('success', `重启虚拟机 ${name}`, '虚拟机已成功重启并恢复至 [运行中]');
          ElMessage.success(`虚拟机 ${name} 重启成功`);
        } else {
          logStore.addLog('error', `重启虚拟机 ${name}`, `已发送指令，当前状态为 [${rebootedVm ? rebootedVm.state : '未知'}]`);
        }
        break;
    }
  } catch (error: any) {
    console.error(`执行 ${actionName} 操作异常`, error);
    const errMsg = error.message || '操作执行失败';
    logStore.addLog('error', `${actionName}虚拟机 ${name}`, errMsg);
  } finally {
    actionLoading.value[name] = false;
  }
};

const executeAction = async (name: string, action: string, actionTitle: string) => {
  actionLoading.value[name] = true;
  try {
    if (action === 'destroy') {
      logStore.addLog('info', `强制关机虚拟机 ${name}`, '正在向虚拟机发送强制断电信号...');
      await destroyVm(name);
      logStore.addLog('success', `强制关机虚拟机 ${name}`, '虚拟机已成功强制断电');
    } else if (action === 'delete') {
      logStore.addLog('info', `删除虚拟机 ${name}`, '正在注销虚拟机定义并清理磁盘映像文件...');
      await deleteVm(name);
      logStore.addLog('success', `删除虚拟机 ${name}`, '虚拟机及磁盘卷已彻底删除');
    }
    ElMessage.success(`${actionTitle}指令执行完成`);
    await fetchData();
  } catch (error: any) {
    console.error(`执行 ${actionTitle} 操作发生错误`, error);
    const errMsg = error.message || '操作失败';
    logStore.addLog('error', `${actionTitle}虚拟机 ${name}`, errMsg);
  } finally {
    actionLoading.value[name] = false;
  }
};

const confirmAction = (name: string, action: string, actionTitle: string) => {
  if (action === 'delete') {
    ElMessageBox.prompt(
      `请输入虚拟机实例名称 "${name}" 以确认注销定义并删除其物理磁盘文件：`,
      '高危删除确认提示',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        inputPattern: new RegExp(`^${name}$`),
        inputErrorMessage: '输入的名称不匹配，请重新输入',
        type: 'error'
      }
    ).then(async () => {
      executeAction(name, action, actionTitle);
    }).catch(() => {});
  } else {
    ElMessageBox.confirm(
      `确定要对虚拟机 ${name} 执行 ${actionTitle} 操作吗？该操作可能导致数据未保存。`,
      '安全警告提示',
      {
        confirmButtonText: '确定执行',
        cancelButtonText: '取消',
        type: 'warning'
      }
    ).then(async () => {
      executeAction(name, action, actionTitle);
    }).catch(() => {});
  }
};

watch(() => route.query.search, (newSearch) => {
  searchQuery.value = (newSearch as string) || '';
});

onMounted(() => {
  fetchData();
  if (route.query.search) {
    searchQuery.value = route.query.search as string;
  }
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
