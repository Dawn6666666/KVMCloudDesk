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
    addLog(type: 'info' | 'success' | 'error', action: string, detail: string, id?: string) {
      const now = new Date();
      const timeStr = now.toLocaleTimeString() + '.' + String(now.getMilliseconds()).padStart(3, '0');
      this.logs.unshift({
        id: id || Math.random().toString(36).substring(2, 9),
        timestamp: timeStr,
        type,
        action,
        detail,
      });
      if (this.logs.length > 200) {
        this.logs = this.logs.slice(0, 200);
      }
    },
    updateLog(id: string, type: 'success' | 'error', detail: string) {
      const log = this.logs.find(l => l.id === id);
      if (log) {
        log.type = type;
        log.detail = detail;
        const now = new Date();
        log.timestamp = now.toLocaleTimeString() + '.' + String(now.getMilliseconds()).padStart(3, '0');
      }
    },
    clearLogs() {
      this.logs = [];
    }
  }
});
