import { App, message, notification, Modal } from 'ant-design-vue'

export const useGlobalStore = defineStore('global', {
  state: () => {
    const staticFunction = App.useApp()

    message.config({
      maxCount: 2,
      duration: 2,
    })
    notification.config({
      maxCount: 2,
      duration: 2,
    })
    return {
      message: message,
      notification: notification,
      modal: Modal
    }
  }
})