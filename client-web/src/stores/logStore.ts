import { defineStore } from 'pinia';

export interface LogEntry {
  id: string;
  timestamp: string;
  type: 'info' | 'success' | 'error';
  action: string;
  detail: string;
}

export const useLogStore = defineStore('log', {
  state: () => ({
    logs: [] as LogEntry[],
  }),
  actions: {
    addLog(type: 'info' | 'success' | 'error', action: string, detail: string) {
      const now = new Date();
      const timeStr = now.toLocaleTimeString() + '.' + String(now.getMilliseconds()).padStart(3, '0');
      this.logs.unshift({
        id: Math.random().toString(36).substring(2, 9),
        timestamp: timeStr,
        type,
        action,
        detail,
      });
      if (this.logs.length > 200) {
        this.logs = this.logs.slice(0, 200);
      }
    },
    clearLogs() {
      this.logs = [];
    }
  }
});
