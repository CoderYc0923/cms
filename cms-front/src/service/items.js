import request from '@/utils/request'

/** @deprecated 树数据请用 spaces/{slug}/tree；保留空实现避免旧引用报错 */
export async function getItems (params) {
  const slug = params?.source || params?.slug
  if (!slug) {
    return { code: 200, data: [] }
  }
  return request(`/api/admin/spaces/${slug}/tree`, {
    method: 'GET'
  })
}

/** 创建文章/菜单条目节点 */
export async function createItem (params) {
  return request('/api/admin/nodes', {
    method: 'POST',
    params
  })
}

/** 编辑条目节点 */
export async function editItem (params, id) {
  return request(`/api/admin/nodes/${id}`, {
    method: 'PUT',
    params
  })
}

/** 删除条目节点 */
export async function deleteItem (id) {
  return request(`/api/admin/nodes/${id}`, {
    method: 'DELETE'
  })
}

/** 读/存正文 */
export async function getArticle (id) {
  return request(`/api/admin/articles/${id}`, {
    method: 'GET'
  })
}

export async function saveArticle (id, params) {
  return request(`/api/admin/articles/${id}`, {
    method: 'PUT',
    params
  })
}

export async function publishArticle (id) {
  return request(`/api/admin/articles/${id}/publish`, {
    method: 'POST',
    params: {}
  })
}

export async function unpublishArticle (id) {
  return request(`/api/admin/articles/${id}/unpublish`, {
    method: 'POST',
    params: {}
  })
}
