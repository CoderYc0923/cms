import { createRouter, createWebHistory } from 'vue-router'
import DocsLayout from '@/docs/layouts/DocsLayout.vue'

const routes = [
  {
    path: '/',
    component: DocsLayout,
    children: [
      {
        path: '',
        name: 'DocsHome',
        component: () => import('@/docs/views/DocsHome.vue'),
        meta: { title: '文档首页' }
      },
      {
        path: 'articles/:nodeId',
        name: 'DocsArticle',
        component: () => import('@/docs/views/DocsHome.vue'),
        meta: { title: '文档' }
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
