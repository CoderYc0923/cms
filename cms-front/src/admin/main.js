import App from './App.vue'
import router from './router.js'
import { bootstrapApp } from '@/shared/bootstrap.js'
import { normalizeHtmlEntryPath } from '@/shared/normalizeHtmlEntryPath.js'

normalizeHtmlEntryPath()

bootstrapApp({
  App,
  router
})
