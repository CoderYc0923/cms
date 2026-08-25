import { createRouter, createWebHistory } from 'vue-router'
import AdminLayout from '@/admin/layouts/AdminLayout.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/admin/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: AdminLayout,
    redirect: '/shopchup',
    children: [
      {
        path: 'shopchup',
        name: 'Shopchup',
        component: () => import('@/views/Shopchup/index.vue'),
        meta: { title: 'Shopchup' }
      },
      {
        path: 'iot',
        name: 'Iot',
        component: () => import('@/views/Iot/index.vue'),
        meta: { title: '物联网' }
      }
    ]
  },
  {
    path: '/404',
    component: () => import('@/views/exception/404.vue')
  },
  {
    path: '/:catchAll(.*)',
    redirect: '/404'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
