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
    redirect: '/spaces',
    children: [
      {
        path: 'spaces',
        name: 'SpaceManage',
        component: () => import('@/admin/views/SpaceManage.vue'),
        meta: { title: '空间管理' }
      },
      {
        path: 'shopchup',
        name: 'Shopchup',
        component: () => import('@/views/SpaceWorkspace/index.vue'),
        meta: { title: 'Shopchup', spaceSlug: 'shopchup' }
      },
      {
        path: 'iot',
        name: 'Iot',
        component: () => import('@/views/SpaceWorkspace/index.vue'),
        meta: { title: '物联网', spaceSlug: 'iot' }
      },
      {
        path: ':spaceSlug',
        name: 'SpaceDynamic',
        component: () => import('@/views/SpaceWorkspace/index.vue'),
        meta: { title: '空间' }
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
