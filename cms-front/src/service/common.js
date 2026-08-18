import request from '@/utils/request'

/** 公开目录树（docs / 当前 Catalogue 拉树仍走 public） */
export async function getDirectoryTree (slug) {
  return request(`/api/public/spaces/${slug}/tree`, {
    method: 'GET'
  })
}

/** 公开文章正文 */
export async function getPublicArticle (slug, id) {
  return request(`/api/public/spaces/${slug}/articles/${id}`, {
    method: 'GET'
  })
}

export async function downloadFile (params) {
  return request('/api/admin/files/get', { method: 'GET', params })
}

export async function getPreviewImg (params) {
  return request('/api/admin/files/preview', {
    method: 'GET',
    params
  })
}
