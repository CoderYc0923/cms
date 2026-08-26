import { message, notification, Modal } from 'ant-design-vue'

/**
 * 只挂静态 API，不要在 store 里调用 App.useApp()：
 * useApp 必须在 <a-app> 子树的 setup 中使用，否则容易启动白屏。
 */
export const useGlobalStore = defineStore('global', {
  state: () => {
    message.config({
      maxCount: 2,
      duration: 2
    })
    notification.config({
      maxCount: 2,
      duration: 2
    })
    return {
      message,
      notification,
      modal: Modal
    }
  }
})
