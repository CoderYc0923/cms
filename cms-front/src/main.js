import '@ant-design-vue/pro-layout/dist/style.css' // pro-layout css or style.less
import infiniteScroll from 'vue3-infinite-scroll-better'

import { createApp } from 'vue'
import App from './App.vue'
import Antd from 'ant-design-vue'
import ProLayout, { PageContainer } from '@ant-design-vue/pro-layout'
import { components } from './components/index'
import { directives } from './directives/index'
// 路由
import router from './router'
// pinia
import { createPinia } from 'pinia'

import './assets/style/global.less'
import './router/permission'

const pinia = createPinia()
const app = createApp(App)

// 注册全局组件
components(app)
// 注册全局指令
directives(app)

app.use(pinia)
app.use(router)

app.use(Antd)
app.use(ProLayout)
app.use(PageContainer)
app.use(infiniteScroll)

app.mount('#app')
