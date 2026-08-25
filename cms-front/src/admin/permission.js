import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/token'

NProgress.configure({ showSpinner: false })

const WHITE_LIST = ['/login', '/404']

export function setupAdminPermission (router) {
  router.beforeEach((to, from, next) => {
    NProgress.start()

    const token = getToken()
    const isWhite = WHITE_LIST.includes(to.path)

    if (token) {
      if (to.path === '/login') {
        const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/shopchup'
        next(redirect.startsWith('/') && !redirect.startsWith('/login') ? redirect : '/shopchup')
        return
      }
      next()
      return
    }

    if (isWhite) {
      next()
      return
    }

    next({
      path: '/login',
      query: { redirect: to.fullPath }
    })
  })

  router.afterEach(() => {
    NProgress.done()
  })
}
