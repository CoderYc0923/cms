import { createApp } from 'vue'
import Antd from 'ant-design-vue'
import { createPinia } from 'pinia'
import infiniteScroll from 'vue3-infinite-scroll-better'
import { components } from '@/components/index'
import { directives } from '@/directives/index'
import '@/assets/style/global.less'
import '@/shared/styles/tokens.less'

export function bootstrapApp ({ App, router, permission, registerGlobals = true }) {
  const pinia = createPinia()
  const app = createApp(App)

  if (registerGlobals) {
    components(app)
    directives(app)
  }

  app.use(pinia)
  app.use(Antd)
  app.use(infiniteScroll)

  if (router) {
    app.use(router)
  }

  if (permission) {
    permission(router)
  }

  app.mount('#app')
  return app
}
