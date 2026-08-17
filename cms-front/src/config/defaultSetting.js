export const defaultSetting = {
  defaultRoute: '/'
}


/**
 * 项目默认配置项
 * navTheme - sidebar theme ['dark', 'light'] 两种主题
 * colorWeak - 色盲模式
 * layout - 整体布局方式 ['side' | 'top' | 'mix'] 三种布局
 * fixedHeader - 固定 Header : boolean
 * fixSiderbar - 固定左侧菜单栏 ： boolean
 * contentWidth - 内容区布局： 流式 |  固定
 *
 * storageOptions: {} - Vue-ls 插件配置项 (localStorage/sessionStorage)
 *
 */

export default {
  navTheme: 'light',
  layout: 'top',
  splitMenus: true,
  fixedHeader: true,
  fixSiderbar: true,
  title: 'CMS 管理系统',
  pwa: false,
  iconfontUrl: '//at.alicdn.com/t/c/font_4350959_xfvjyqkqf2c.js'
}
