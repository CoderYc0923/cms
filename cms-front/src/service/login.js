import request from '@/utils/request'

// 获取公钥
export async function getPublicKey () {
  return request('/api/auth/public-key', {
    method: 'GET'
  })
}

// 登录
export async function login (params) {
  return request('/api/auth/login', {
    method: 'POST',
    params,
    showDefaultErrMsg: false,
    isShowNoise: false
  })
}
