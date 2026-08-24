import request from '@/utils/request'

export function initUpload (params) {
  return request('/api/admin/files/uploads/init', {
    method: 'POST',
    params,
    timeout: 30000
  })
}

export function signParts (fileId, params) {
  return request(`/api/admin/files/uploads/${fileId}/parts/sign`, {
    method: 'POST',
    params,
    timeout: 30000
  })
}

export function completeUpload (fileId, params = {}) {
  return request(`/api/admin/files/uploads/${fileId}/complete`, {
    method: 'POST',
    params,
    timeout: 30000
  })
}

export function abortUpload (fileId) {
  return request(`/api/admin/files/uploads/${fileId}/abort`, {
    method: 'POST',
    params: {},
    timeout: 30000,
    showDefaultErrMsg: false,
    isShowNoise: false
  })
}
