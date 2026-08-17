import { usePermissionStore } from '@/stores/permission.js'
import { getToken } from '@/utils/token'
import router from './index'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false })

const allowList = [ 'login'] // no redirect allowList
const loginRouteName = 'login'
const defaultRoutePath = '/'

router.beforeEach(async (to, from, next) => {
  NProgress.start()

  const permissionStore = usePermissionStore()

  if (!permissionStore.getInited) {
    // 路由未初始化
    permissionStore.generateRoutes()
    const addRoutes = permissionStore.getAddRoutes
    addRoutes.forEach(route => {
      router.addRoute(route)
    })
    // 请求带有 redirect 重定向时，登录自动重定向到该地址
    const redirect = decodeURIComponent(from.query.redirect || to.path)
    if (to.fullPath === redirect) {
      next({ path: to.path })
    } else {
      // 跳转到目的路由
      next({ path: to.path, query: to.query })
    }
  } else {
    next()
  }

  NProgress.done()
})

/* router.beforeEach(async (to, from, next) => {
  NProgress.start()

  const token = getToken()
  const permissionStore = usePermissionStore()
  if (token) {
    // 登录状态 输入登录地址 直接跳转到默认页
    if (to.name === loginRouteName) {
      next({ path: defaultRoutePath })
      NProgress.done()
    } else {
      if (!permissionStore.getInited) {
        // 路由未初始化
        permissionStore.generateRoutes()
        const addRoutes = permissionStore.getAddRoutes
        addRoutes.forEach(route => {
          router.addRoute(route)
        })
        // 请求带有 redirect 重定向时，登录自动重定向到该地址
        const redirect = decodeURIComponent(from.query.redirect || to.path)
        if (to.fullPath === redirect) {
          next({ path: to.path })
        } else {
          // 跳转到目的路由
          next({ path: to.path, query: to.query })
        }
      } else {
        next()
      }
    }
  } else {
    if (allowList.includes(to.name)) {
      // 在免登录名单，直接进入
      next()
    } else {
      next({ name: loginRouteName, query: { redirect: to.fullPath } })
      NProgress.done()
    }
  }
}) */

router.afterEach(() => {
  NProgress.done()
})

export default router


