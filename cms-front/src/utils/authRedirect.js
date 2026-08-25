import { useUserStore } from '@/stores/user'

/**
 * 未登录或鉴权失效时跳转登录页（Admin MPA 根路径 /login）
 */
export function redirectToLogin () {
  const userStore = useUserStore()
  userStore.resetAuth()

  const { pathname, search } = window.location
  if (pathname === '/login' || pathname.endsWith('/login')) {
    return
  }

  const redirect = pathname + search
  window.location.assign(`/login?redirect=${encodeURIComponent(redirect)}`)
}
