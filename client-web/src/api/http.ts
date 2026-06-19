import axios from 'axios';
import { ElMessage } from 'element-plus';
import { useLogStore } from '@/stores/logStore';

const instance = axios.create({
  baseURL: '/api',
  timeout: 15000,
});

function logAction(type: 'info' | 'success' | 'error', action: string, detail: string) {
  try {
    const logStore = useLogStore();
    logStore.addLog(type, action, detail);
  } catch (e) {
    console.warn('Failed to log action:', e);
  }
}

instance.interceptors.request.use(
  (config) => {
    const actionDesc = config.headers?.['X-Action-Description'];
    if (actionDesc) {
      logAction('info', String(actionDesc), '正在发送请求');
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

instance.interceptors.response.use(
  (response) => {
    const res = response.data;
    const actionDesc = response.config.headers?.['X-Action-Description'];

    if (res && typeof res === 'object' && 'success' in res) {
      if (res.success) {
        if (actionDesc) {
          logAction('success', String(actionDesc), res.message || '操作成功');
        }
        return res.data;
      } else {
        const errMsg = res.message || '业务错误';
        ElMessage.error(errMsg);
        if (actionDesc) {
          logAction('error', String(actionDesc), errMsg);
        }
        return Promise.reject(new Error(errMsg));
      }
    }
    return res;
  },
  (error) => {
    const actionDesc = error.config?.headers?.['X-Action-Description'];
    let errMsg = '网络或系统异常';

    if (error.response) {
      const data = error.response.data;
      errMsg = data?.message || `HTTP 错误: ${error.response.status}`;
    } else if (error.message) {
      errMsg = error.message;
    }

    ElMessage.error(errMsg);
    if (actionDesc) {
      logAction('error', String(actionDesc), errMsg);
    }
    return Promise.reject(error);
  }
);

export default instance;
