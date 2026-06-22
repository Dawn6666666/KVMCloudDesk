<template>
  <div class="networks-container">
    <el-card class="networks-card">
      <template #header>
        <div class="card-header">
          <span>虚拟网络配置与管理</span>
          <div class="header-actions">
            <el-button type="primary" :icon="Plus" @click="openCreateDialog">创建虚拟网络</el-button>
            <el-button :icon="Refresh" @click="fetchData" :loading="globalLoading">刷新</el-button>
          </div>
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
        <el-table-column prop="netmask" label="子网掩码" width="130">
          <template #default="{ row }">
            <span>{{ row.netmask || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="关联虚拟机" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="row.vmCount ? 'primary' : 'info'" size="small">
              {{ row.vmCount || 0 }} 台
            </el-tag>
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="180" fixed="right">
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
              <el-button 
                v-if="!row.active"
                size="small" 
                type="danger" 
                :loading="actionLoading[row.name]"
                @click="confirmDelete(row.name)"
              >
                注销
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建网络 Dialog -->
    <el-dialog 
      v-model="createDialogVisible" 
      title="创建虚拟局域网" 
      width="540px"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="formRef" 
        :model="createForm" 
        :rules="formRules" 
        label-width="110px"
        label-position="left"
      >
        <el-form-item label="网络名称" prop="name">
          <el-input v-model="createForm.name" placeholder="例如: test-net" />
        </el-form-item>
        <el-form-item label="转发模式" prop="forwardMode">
          <el-select v-model="createForm.forwardMode" placeholder="请选择转发模式" style="width: 100%;">
            <el-option label="NAT模式" value="nat" />
            <el-option label="路由模式" value="route" />
            <el-option label="仅隔离模式" value="isolated" />
            <el-option label="无转发模式" value="none" />
          </el-select>
        </el-form-item>
        <el-form-item label="网关 IP 地址" prop="ipAddress">
          <el-input v-model="createForm.ipAddress" placeholder="例如: 192.168.100.1" />
        </el-form-item>
        <el-form-item label="子网掩码" prop="netmask">
          <el-input v-model="createForm.netmask" placeholder="例如: 255.255.255.0" />
        </el-form-item>
        <el-form-item label="启用 DHCP 服务" prop="dhcpEnabled">
          <el-switch v-model="createForm.dhcpEnabled" />
        </el-form-item>
        <template v-if="createForm.dhcpEnabled">
          <el-form-item label="DHCP 起始 IP" prop="dhcpStart">
            <el-input v-model="createForm.dhcpStart" placeholder="例如: 192.168.100.2" />
          </el-form-item>
          <el-form-item label="DHCP 结束 IP" prop="dhcpEnd">
            <el-input v-model="createForm.dhcpEnd" placeholder="例如: 192.168.100.254" />
          </el-form-item>
        </template>
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
import { getNetworks, startNetwork, stopNetwork, createNetwork, deleteNetwork, getVms } from '@/api/kvm';
import type { NetworkInfoDto, CreateNetworkRequest } from '@/types/kvm';

const networks = ref<NetworkInfoDto[]>([]);
const globalLoading = ref(false);
const actionLoading = ref<Record<string, boolean>>({});

const createDialogVisible = ref(false);
const createLoading = ref(false);
const formRef = ref<FormInstance | null>(null);

const createForm = ref<CreateNetworkRequest>({
  name: '',
  forwardMode: 'nat',
  ipAddress: '',
  netmask: '',
  dhcpEnabled: false,
  dhcpStart: '',
  dhcpEnd: ''
});

const ipPattern = /^((25[0-5]|2[0-4]\d|[01]?\d\d?)\.){3}(25[0-5]|2[0-4]\d|[01]?\d\d?)$/;

const formRules: FormRules = {
  name: [
    { required: true, message: '请输入局域网网络名称', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: '仅能使用英文字母、数字、下划线和连字符', trigger: 'blur' }
  ],
  forwardMode: [{ required: true, message: '请选择转发模式', trigger: 'change' }],
  ipAddress: [
    { pattern: ipPattern, message: '请输入合法的 IP 地址格式', trigger: 'blur' }
  ],
  netmask: [
    { pattern: ipPattern, message: '请输入合法的子网掩码格式', trigger: 'blur' }
  ],
  dhcpStart: [
    { required: true, message: '请输入 DHCP 起始 IP', trigger: 'blur' },
    { pattern: ipPattern, message: '请输入合法的 IP 地址格式', trigger: 'blur' }
  ],
  dhcpEnd: [
    { required: true, message: '请输入 DHCP 结束 IP', trigger: 'blur' },
    { pattern: ipPattern, message: '请输入合法的 IP 地址格式', trigger: 'blur' }
  ]
};

const fetchData = async () => {
  globalLoading.value = true;
  try {
    const [netsData, vmsData] = await Promise.all([
      getNetworks(),
      getVms()
    ]);
    networks.value = netsData.map(net => {
      return {
        ...net,
        vmCount: vmsData.filter(vm => vm.networkName === net.name).length
      };
    });
  } catch (error) {
    console.error('加载虚拟网络列表或虚拟机列表异常', error);
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

const openCreateDialog = () => {
  createDialogVisible.value = true;
  createForm.value = {
    name: '',
    forwardMode: 'nat',
    ipAddress: '',
    netmask: '',
    dhcpEnabled: false,
    dhcpStart: '',
    dhcpEnd: ''
  };
};

const submitCreate = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      createLoading.value = true;
      try {
        await createNetwork(createForm.value);
        ElMessage.success('虚拟网络定义并启动成功');
        createDialogVisible.value = false;
        fetchData();
      } catch (error) {
        console.error('新建虚拟网络失败', error);
      } finally {
        createLoading.value = false;
      }
    }
  });
};

const confirmDelete = (name: string) => {
  ElMessageBox.confirm(
    `确定要注销虚拟网络 ${name} 吗？该操作会将网络配置从宿主机中彻底移除。`,
    '网络注销警告',
    {
      confirmButtonText: '确定注销',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    actionLoading.value[name] = true;
    try {
      await deleteNetwork(name);
      ElMessage.success('虚拟网络注销成功');
      await fetchData();
    } catch (error) {
      console.error('注销虚拟网络异常', error);
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

.header-actions {
  display: flex;
  gap: 10px;
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
