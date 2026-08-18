import { login, logout } from '@/service/login'
import { clearToken, getToken, setToken, clearRefreshToken, setRefreshToken, getRefreshToken } from '@/utils/token'

const getStoredUserInfo = () => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
}

const isOk = code => code === 0 || code === 200

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: getStoredUserInfo(),
    isLoggedIn: !!getToken()
  }),
  getters: {
    getUserInfo: state => state.userInfo
  },
  actions: {
    login (params) {
      return new Promise((resolve, reject) => {
        login({
          username: params.username,
          password: params.password
        }).then(response => {
          if (isOk(response.code)) {
            const data = response.data || {}
            setToken(data.accessToken)
            if (data.refreshToken) {
              setRefreshToken(data.refreshToken)
            }
            this.userInfo = { username: params.username }
            this.isLoggedIn = true
            localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
            resolve(response)
          } else {
            reject(response.message)
          }
        }).catch(err => {
          reject(err)
        })
      })
    },
    loginOut () {
      const refreshToken = getRefreshToken()
      const done = () => {
        this.resetAuth()
      }
      if (!refreshToken) {
        done()
        return Promise.resolve()
      }
      return logout(refreshToken).catch(() => {}).finally(done)
    },
    resetAuth () {
      clearToken()
      clearRefreshToken()
      this.userInfo = {}
      this.isLoggedIn = false
      localStorage.removeItem('userInfo')
    }
  }
})
