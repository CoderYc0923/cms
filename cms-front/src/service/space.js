import request from '@/utils/request'

/** 空间列表（管理端） */
export async function getSpaceList (status) {
  return request('/api/admin/spaces', {
    method: 'GET',
    params: status != null ? { status } : undefined
  })
}

/** 创建空间 */
export async function createSpace (params) {
  return request('/api/admin/spaces', {
    method: 'POST',
    params
  })
}

/** 更新空间 */
export async function updateSpace (id, params) {
  return request(`/api/admin/spaces/${id}`, {
    method: 'PUT',
    params
  })
}
