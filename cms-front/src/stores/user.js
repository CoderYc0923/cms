import { login } from '@/service/login'
import { clearToken, getToken, setToken } from '@/utils/token'
import { getPublicKey } from '@/service/login'
import { rsaEncrypt } from '@/utils/crypto'

const getStoredUserInfo = () => {
  try {
    return JSON.parse(localStorage.getItem('userInfo') || '{}')
  } catch {
    return {}
  }
}

export const useUserStore = defineStore('user', {
  state: () => ({
    userInfo: getStoredUserInfo(),
    isLoggedIn: !!getToken()
  }),
  getters: {
    getUserInfo: state => state.userInfo
  },
  actions: {
    encryptInfo (params) {
      return new Promise(async (resolve, reject) => {
        const res = await getPublicKey();
        const key = res.data.publicKey
        const encryptedUsername = await rsaEncrypt(params.username, key);
        const encryptedPassword = await rsaEncrypt(params.password, key);
        resolve({
          encryptedUsername: encryptedUsername,
          encryptedPassword: encryptedPassword
        })
      })
    },
    login (params) {
      return new Promise(async (resolve, reject) => {
        const encryptedParams = await this.encryptInfo(params);
        login(encryptedParams).then(response => {
            if (!response.code) {
              setToken(response.data.token)
              this.userInfo = response.data.user
              this.isLoggedIn = true
              localStorage.setItem('userInfo', JSON.stringify(this.userInfo))
            } else {
              reject(response.message)
            }
            resolve(response)
          }).catch(err => {
            reject(err)
          })
        })
    },
    loginOut () {
      return new Promise((resolve) => {
        this.resetAuth()
        resolve()
      })
    },
    resetAuth () {
      clearToken()
      this.userInfo = {}
      this.isLoggedIn = false
      localStorage.removeItem('userInfo')
    }
  }
})