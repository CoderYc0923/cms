import { constantRouterMap, asyncRouterMap } from '../config/router.config'

export const usePermissionStore = defineStore('permission', {
  state: () => ({ routes: constantRouterMap, addRoutes: [], hasInited: false }),
  getters: {
    getRoutes (state) {
      return state.routes
    },
    getAddRoutes (state) {
      return state.addRoutes
    },
    getInited (state) {
      return state.hasInited
    }
  },
  actions: {
    generateRoutes () {
      this.addRoutes = asyncRouterMap
      this.routes = this.routes.concat(asyncRouterMap)
      this.hasInited = true
    }
  }
})