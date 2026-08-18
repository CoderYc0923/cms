import request from '@/utils/request'

/** 登录：明文 username / password → /api/admin/auth/login */
export async function login (params) {
  return request('/api/admin/auth/login', {
    method: 'POST',
    params,
    showDefaultErrMsg: false,
    isShowNoise: false
  })
}

export async function refresh (refreshToken) {
  return request('/api/admin/auth/refresh', {
    method: 'POST',
    params: { refreshToken },
    showDefaultErrMsg: false,
    isShowNoise: false
  })
}

export async function logout (refreshToken) {
  return request('/api/admin/auth/logout', {
    method: 'POST',
    params: { refreshToken },
    showDefaultErrMsg: false,
    isShowNoise: false
  })
}
