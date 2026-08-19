import { usePermissionStore } from '@/stores/permission.js'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'

NProgress.configure({ showSpinner: false })

export function setupAdminPermission (router) {
  router.beforeEach(async (to, from, next) => {
    NProgress.start()

    const permissionStore = usePermissionStore()

    if (!permissionStore.getInited) {
      permissionStore.generateRoutes()
      const addRoutes = permissionStore.getAddRoutes
      addRoutes.forEach(route => {
        router.addRoute(route)
      })
      const redirect = decodeURIComponent(from.query.redirect || to.path)
      if (to.fullPath === redirect) {
        next({ path: to.path })
      } else {
        next({ path: to.path, query: to.query })
      }
    } else {
      next()
    }

    NProgress.done()
  })

  router.afterEach(() => {
    NProgress.done()
  })
}
