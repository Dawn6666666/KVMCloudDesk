<template>
  <div class="images-container">
    <el-card class="images-card">
      <template #header>
        <div class="card-header">
          <span>操作系统镜像目录</span>
          <div class="header-right">
            <el-button type="primary" :icon="Plus" @click="openCreateDialog">添加系统镜像</el-button>
            <el-button :icon="Refresh" @click="fetchData" :loading="globalLoading">刷新</el-button>
          </div>
        </div>
      </template>

      <el-table v-loading="globalLoading" :data="images" style="width: 100%">
        <el-table-column prop="name" label="镜像名称" width="180" fixed />
        <el-table-column prop="format" label="格式" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="getFormatTagType(row.format)" effect="dark">
              {{ (row.format || 'unknown').toUpperCase() }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="占用 / 规格" width="180">
          <template #default="{ row }">
            <span>{{ (row.physicalSizeGb || 0).toFixed(2) }} GB / {{ (row.sizeGb || 0).toFixed(2) }} GB</span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="物理路径" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="path-cell" style="display: flex; align-items: center; gap: 8px;">
              <span :class="['status-dot', row.exists ? 'active' : 'error']" :title="row.exists ? '物理文件完好' : '物理文件缺失'"></span>
              <span>{{ row.path }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="扫描录入时间" width="160" />
        <el-table-column prop="description" label="详细描述" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button 
              size="small" 
              type="danger" 
              plain
              :loading="actionLoading[row.name]"
              @click="confirmDelete(row.name)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- Add Image Dialog -->
    <el-dialog 
      v-model="createDialogVisible" 
      title="录入系统镜像" 
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form 
        ref="formRef" 
        :model="createForm" 
        :rules="formRules" 
        label-width="90px"
        label-position="left"
      >
        <el-form-item label="镜像名称" prop="name">
          <el-input v-model="createForm.name" placeholder="例如: centos7" />
        </el-form-item>
        <el-form-item label="文件路径" prop="path">
          <el-input v-model="createForm.path" placeholder="在宿主机的绝对路径，例如: /var/lib/libvirt/images/centos7.qcow2">
            <template #append>
              <el-button @click="openSelectVolumeDialog">从存储池选择</el-button>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="详细描述" prop="description">
          <el-input 
            v-model="createForm.description" 
            type="textarea" 
            rows="3" 
            placeholder="说明系统版本及所含基础软件" 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="createDialogVisible = false">取消</el-button>
          <el-button type="primary" :loading="createLoading" @click="submitCreate">确认录入</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 存储卷选择 Dialog -->
    <el-dialog
      v-model="volumeDialogVisible"
      title="选择宿主机映像文件 (默认存储池)"
      width="600px"
      :close-on-click-modal="false"
    >
      <el-table 
        v-loading="volumeLoading" 
        :data="availableVolumes" 
        style="width: 100%"
        highlight-current-row
        @row-dblclick="selectVolumeRow"
      >
        <el-table-column prop="name" label="卷名称" min-width="150" />
        <el-table-column label="容量" width="120">
          <template #default="{ row }">
            {{ (row.capacityGb || 0).toFixed(1) }} GB
          </template>
        </el-table-column>
        <el-table-column prop="path" label="宿主机绝对路径" min-width="260" show-overflow-tooltip />
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="selectVolumeRow(row)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Plus, Refresh } from '@element-plus/icons-vue';
import { getImages, addImage, deleteImage, getStoragePoolVolumes } from '@/api/kvm';
import type { ImageInfoDto, AddImageRequest } from '@/types/kvm';

const getFormatTagType = (format: string) => {
  if (!format) return 'info';
  const lower = format.toLowerCase();
  if (lower === 'qcow2') return 'primary';
  if (lower === 'raw') return 'warning';
  if (lower === 'iso') return 'success';
  return 'info';
};

const images = ref<ImageInfoDto[]>([]);
const globalLoading = ref(false);
const createLoading = ref(false);
const createDialogVisible = ref(false);
const actionLoading = ref<Record<string, boolean>>({});

const volumeDialogVisible = ref(false);
const volumeLoading = ref(false);
const availableVolumes = ref<any[]>([]);

const openSelectVolumeDialog = async () => {
  volumeDialogVisible.value = true;
  volumeLoading.value = true;
  try {
    const data = await getStoragePoolVolumes('default');
    availableVolumes.value = data;
  } catch (error) {
    console.error('获取存储池卷失败', error);
    ElMessage.error('无法读取存储池下的物理文件列表');
  } finally {
    volumeLoading.value = false;
  }
};

const selectVolumeRow = (row: any) => {
  createForm.value.path = row.path;
  const filename = row.name;
  const lastDotIndex = filename.lastIndexOf('.');
  if (lastDotIndex > 0) {
    createForm.value.name = filename.substring(0, lastDotIndex);
  } else {
    createForm.value.name = filename;
  }
  volumeDialogVisible.value = false;
};

const formRef = ref<FormInstance | null>(null);

const createForm = ref<AddImageRequest>({
  name: '',
  path: '',
  description: ''
});

const formRules: FormRules = {
  name: [
    { required: true, message: '请填写镜像别名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+$/, message: '名称必须由字母、数字或下划线连字符组成', trigger: 'blur' }
  ],
  path: [
    { required: true, message: '请配置物理绝对路径', trigger: 'blur' }
  ]
};

const fetchData = async () => {
  globalLoading.value = true;
  try {
    const data = await getImages();
    images.value = data;
  } catch (error) {
    console.error('加载系统镜像列表异常', error);
  } finally {
    globalLoading.value = false;
  }
};

const openCreateDialog = () => {
  createDialogVisible.value = true;
  createForm.value = {
    name: '',
    path: '',
    description: ''
  };
};

const submitCreate = async () => {
  if (!formRef.value) return;
  await formRef.value.validate(async (valid) => {
    if (valid) {
      createLoading.value = true;
      try {
        await addImage(createForm.value);
        ElMessage.success('镜像配置成功');
        createDialogVisible.value = false;
        fetchData();
      } catch (error) {
        console.error('配置基础镜像异常', error);
      } finally {
        createLoading.value = false;
      }
    }
  });
};

const confirmDelete = (name: string) => {
  ElMessageBox.confirm(
    `确定要从平台注销镜像 ${name} 吗？这仅仅会删除数据库中的配置，不会删除硬盘里的物理文件。`,
    '安全移除镜像确认',
    {
      confirmButtonText: '确定移除',
      cancelButtonText: '取消',
      type: 'warning'
    }
  ).then(async () => {
    actionLoading.value[name] = true;
    try {
      await deleteImage(name);
      ElMessage.success('系统镜像注销完成');
      await fetchData();
    } catch (error) {
      console.error('注销镜像错误', error);
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
.images-container {
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

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  display: inline-block;
}
.status-dot.active {
  background-color: var(--el-color-success);
  box-shadow: 0 0 6px var(--el-color-success);
}
.status-dot.error {
  background-color: var(--el-color-danger);
  box-shadow: 0 0 6px var(--el-color-danger);
}
</style>
