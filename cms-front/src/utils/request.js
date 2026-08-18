import axios from 'axios'
import qs from 'qs'
import notification from 'ant-design-vue/es/notification'
import { checkStatus, checkCode } from './check'
import { LOGIN_CODE_MAP } from '@/consts/codeEnum'
import { getToken, getRefreshToken, setToken, setRefreshToken } from './token'
import { useUserStore } from '@/stores/user'
import { API_PREFIX } from '@/consts/const'

const AUTH_SKIP_REFRESH = /\/api\/admin\/auth\/(login|refresh|logout)(?:\?|$)/

const headerDefaultContentType = {
  GET: 'application/x-www-form-urlencoded; charset=utf-8',
  POST: 'application/json; charset=utf-8',
  DELETE: 'application/x-www-form-urlencoded; charset=utf-8',
  PUT: 'application/json; charset=utf-8',
  PATCH: 'application/json; charset=utf-8'
}

const instance = axios.create({
  // eslint-disable-next-line no-undef
  baseURL: process.env.VUE_APP_API_BASE_URL,
  timeout: 6000
})

/** 并发 401 只打一次 refresh */
let refreshPromise = null

const isOk = code => code === 0 || code === 200

function refreshTokens () {
  if (!refreshPromise) {
    const refreshToken = getRefreshToken()
    if (!refreshToken) {
      return Promise.reject(new Error('no refresh token'))
    }
    refreshPromise = instance.request({
      url: `${API_PREFIX}/api/admin/auth/refresh`,
      method: 'POST',
      data: { refreshToken },
      headers: { 'Content-Type': 'application/json; charset=utf-8' },
      _skipAuthRefresh: true
    }).then(res => {
      const payload = res.data || {}
      const data = payload.data || {}
      if (!isOk(payload.code) || !data.accessToken) {
        throw new Error(payload.message || 'refresh failed')
      }
      setToken(data.accessToken)
      if (data.refreshToken) {
        setRefreshToken(data.refreshToken)
      }
    }).finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

instance.interceptors.request.use(config => {
  if (config._skipAuthRefresh) {
    return config
  }
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, error => {
  notification.error({
    message: '请求错误',
    description: error.message
  })
})

instance.interceptors.response.use(response => {
  if (Object.values(LOGIN_CODE_MAP).includes(response.data?.code)) {
    useUserStore().resetAuth()
  }
  return response
}, async error => {
  const config = error.config || {}
  const status = error.response?.status
  const url = config.url || ''
  if (status !== 401 || config._skipAuthRefresh || AUTH_SKIP_REFRESH.test(url)) {
    return Promise.reject(error)
  }
  if (config._retry) {
    useUserStore().resetAuth()
    return Promise.reject(error)
  }
  config._retry = true
  try {
    await refreshTokens()
    const token = getToken()
    if (token) {
      config.headers = config.headers || {}
      config.headers.Authorization = `Bearer ${token}`
    }
    return instance.request(config)
  } catch (e) {
    useUserStore().resetAuth()
    return Promise.reject(error)
  }
})

export default (url, options) => {
  const { params, method, headers, showDefaultErrMsg = true, isShowNoise = true, timeout = 20000 } = options
  const config = {
    method,
    timeout,
    responseType: 'json',
    headers: {
      'Content-Type': headerDefaultContentType[method],
      ...headers
    }
  }
  let body = params
  let formData = new FormData()
  let requestUrl = API_PREFIX + url
  if (['POST', 'PUT', 'PATCH', 'DELETE'].includes(method)) {
    if (config.headers['Content-Type'] !== 'multipart/form-data') {
      body = JSON.stringify(params)
    } else {
      formData = params
    }
  } else {
    if (params) {
      const paramsUrl = qs.stringify(params)
      requestUrl = paramsUrl ? `${requestUrl}?${paramsUrl}` : requestUrl
    }
    body = null
  }

  return new Promise((resolve, reject) => {
    instance.request({
      url: requestUrl,
      data: body,
      formData: formData,
      ...config
    }).then(res => {
      if (showDefaultErrMsg) {
        const { code } = res.data
        switch (code) {
        case 0:
        case 200:
          resolve(res.data)
          break
        default:
          if (isShowNoise) checkCode(res.data)
          reject(res.data)
        }
        return
      }
      resolve(res.data)
    }).catch(err => {
      reject(err)
      checkStatus(err.response)
    })
  })
}
