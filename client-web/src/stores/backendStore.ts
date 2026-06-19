import { defineStore } from 'pinia';
import { getHostInfo } from '@/api/kvm';
import type { HostInfoDto } from '@/types/kvm';

export const useBackendStore = defineStore('backend', {
  state: () => ({
    connected: false,
    hostInfo: null as HostInfoDto | null,
    loading: false,
  }),
  actions: {
    async fetchHostInfo() {
      this.loading = true;
      try {
        const info = await getHostInfo();
        this.hostInfo = info;
        this.connected = true;
      } catch (error) {
        this.hostInfo = null;
        this.connected = false;
        throw error;
      } finally {
        this.loading = false;
      }
    }
  }
});
