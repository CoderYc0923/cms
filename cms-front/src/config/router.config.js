import { BasicLayout } from "@/layouts";

export const asyncRouterMap = [
  {
    path: "/",
    name: "index",
    component: BasicLayout,
    redirect: "shopchup",
    children: [
      {
        path: "shopchup",
        name: "Shopchup",
        component: () => import("@/views/Shopchup/index.vue"),
        meta: { title: "Shopchup" },
      },
      {
        path: "iot",
        name: "Iot",
        component: () => import("@/views/Iot/index.vue"),
        meta: { title: "物联网" },
      },
      /* {
        path: "action/:id?",
        name: "Action",
        component: () => import("@/views/Common/Action.vue"),
        meta: { hideInMenu: true, needBack: true },
        beforeEnter: (to, from, next) => {
          to.meta.title = to.params.id ? '编辑' : '创建'
          next()
        },
      },
      {
        path: "preview/:id?",
        name: "Preview",
        component: () => import("@/views/Common/Preview.vue"),
        meta: { title: "预览", hideInMenu: true, needBack: true },
      }, */
    ],
  },
  {
    path: "/:catchAll(.*)",
    redirect: "404",
    hidden: true,
  },
];

/**
 * 基础路由
 * @type { *[] }
 */
export const constantRouterMap = [
  /* {
    path: '/login',
    name: 'login',
    component: () => import('@/views/User/Login.vue'),
    meta: { title: '登录' }
  }, */
  {
    path: "/404",
    component: () =>
      import(/* webpackChunkName: "fail" */ "@/views/exception/404.vue"),
  },
];
