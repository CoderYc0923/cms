import axios from 'axios'
import qs from 'qs'
import notification from 'ant-design-vue/es/notification'
import { checkStatus, checkCode } from './check'
import router from '@/router'
import { LOGIN_CODE_MAP } from '@/consts/codeEnum'
import { getToken } from './token'
import { useUserStore } from '@/stores/user'
import { API_PREFIX } from '@/consts/const'

const headerDefaultContentType = {
  GET: 'application/x-www-form-urlencoded; charset=utf-8',
  POST: 'application/json; charset=utf-8',
  DELETE: 'application/x-www-form-urlencoded; charset=utf-8',
  PUT: 'application/json; charset=utf-8',
  PATCH: 'application/json; charset=utf-8'
}

// 创建 axios 实例
const instance = axios.create({
  // API 请求的默认前缀
  // eslint-disable-next-line no-undef
  baseURL: process.env.VUE_APP_API_BASE_URL,
  timeout: 6000 // 请求超时时间
})

instance.interceptors.request.use(config => {
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
  if (Object.values(LOGIN_CODE_MAP).includes(response.data.code)) {
    useUserStore().resetAuth()
  }
  return response
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
          resolve(res.data)
          break
        default:
          if (isShowNoise) checkCode(res.data)
          reject(res.data)
        }
      }
      resolve(res.data)
    }).catch(err => {
      reject(err)
      checkStatus(err.response)
    })
  })
}
