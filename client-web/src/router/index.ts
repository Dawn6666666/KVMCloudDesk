import { createRouter, createWebHistory } from 'vue-router';
import MainLayout from '@/layouts/MainLayout.vue';

const routes = [
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: '',
        name: 'Dashboard',
        component: () => import('@/views/DashboardView.vue'),
      },
      {
        path: 'host',
        name: 'Host',
        component: () => import('@/views/HostView.vue'),
      },
      {
        path: 'vms',
        name: 'VMs',
        component: () => import('@/views/VmView.vue'),
      },
      {
        path: 'images',
        name: 'Images',
        component: () => import('@/views/ImageView.vue'),
      },
      {
        path: 'networks',
        name: 'Networks',
        component: () => import('@/views/NetworkView.vue'),
      },
      {
        path: 'snapshots',
        name: 'Snapshots',
        component: () => import('@/views/SnapshotView.vue'),
      },
      {
        path: 'storage',
        name: 'Storage',
        component: () => import('@/views/StorageView.vue'),
      },
    ]
  }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
});

export default router;
