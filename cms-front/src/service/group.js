import request from '@/utils/request'

// 获取目录树
export async function getGroupList (source) {
  return request('/api/groups', {
    method: 'GET',
    params: {
      source
    }
  })
}

// 新增分组
export async function addGroup (params) {
  return request('/api/groups', {
    method: 'POST',
    params
  })
}

// 编辑分组
export async function editGroup (params, id) {
  return request(`/api/groups/${id}`, {
    method: 'PUT',
    params
  })
}

// 删除分组
export async function deleteGroup (id) {
  return request(`/api/groups/${id}`, {
    method: 'DELETE'
  })
}