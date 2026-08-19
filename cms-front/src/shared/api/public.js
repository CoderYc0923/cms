import request from '@/utils/request'

/** 公开目录树（docs 只读） */
export async function getPublicTree (slug) {
  return request(`/api/public/spaces/${slug}/tree`, {
    method: 'GET'
  })
}

/** 公开文章正文 */
export async function getPublicArticle (slug, nodeId) {
  return request(`/api/public/spaces/${slug}/articles/${nodeId}`, {
    method: 'GET'
  })
}
