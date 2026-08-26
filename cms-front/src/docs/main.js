import App from './App.vue'
import router from './router.js'
import { bootstrapApp } from '@/shared/bootstrap.js'
import { normalizeHtmlEntryPath } from '@/shared/normalizeHtmlEntryPath.js'
import { setupEmbedBridge } from '@/docs/utils/embed.js'

normalizeHtmlEntryPath()

bootstrapApp({
  App,
  router,
  registerGlobals: false
})

setupEmbedBridge(router)
