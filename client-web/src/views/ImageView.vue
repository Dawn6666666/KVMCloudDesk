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
            <el-tag size="small" type="info" effect="dark">
              {{ (row.format || 'unknown').toUpperCase() }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="容量">
          <template #default="{ row }">
            <span>{{ row.sizeGb.toFixed(2) }} GB</span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="物理路径" min-width="260" show-overflow-tooltip />
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
          <el-input v-model="createForm.path" placeholder="在宿主机的绝对路径，例如: /var/lib/libvirt/images/centos7.qcow2" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { ElMessageBox, ElMessage } from 'element-plus';
import type { FormInstance, FormRules } from 'element-plus';
import { Plus, Refresh } from '@element-plus/icons-vue';
import { getImages, addImage, deleteImage } from '@/api/kvm';
import type { ImageInfoDto, AddImageRequest } from '@/types/kvm';

const images = ref<ImageInfoDto[]>([]);
const globalLoading = ref(false);
const createLoading = ref(false);
const createDialogVisible = ref(false);
const actionLoading = ref<Record<string, boolean>>({});

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
</style>
