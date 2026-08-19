import request from '@/utils/request'

export { getPublicTree as getDirectoryTree, getPublicArticle } from '@/shared/api/public'

export async function downloadFile (params) {
  return request('/api/admin/files/get', { method: 'GET', params })
}

export async function getPreviewImg (params) {
  return request('/api/admin/files/preview', {
    method: 'GET',
    params
  })
}
