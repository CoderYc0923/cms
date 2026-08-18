import request from '@/utils/request'

/** 管理端完整目录树（含草稿）；slug 即原 source */
export async function getGroupList (slug) {
  return request(`/api/admin/spaces/${slug}/tree`, {
    method: 'GET'
  })
}

/** 新增分组 / 菜单节点 */
export async function addGroup (params) {
  return request('/api/admin/nodes', {
    method: 'POST',
    params
  })
}

/** 编辑节点 */
export async function editGroup (params, id) {
  return request(`/api/admin/nodes/${id}`, {
    method: 'PUT',
    params
  })
}

/** 删除节点 */
export async function deleteGroup (id) {
  return request(`/api/admin/nodes/${id}`, {
    method: 'DELETE'
  })
}
