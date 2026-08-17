import request from '@/utils/request'

// 获取某分组下条目
export async function getItems(params) {
    return request('/api/items', {
        method: 'GET',
        params
    })
}

// 创建条目
export async function createItem(params) {
    return request('/api/items', {
        method: 'POST',
        params
    })
}

// 编辑条目
export async function editItem(params, id) {
    return request(`/api/items/${id}`, {
        method: 'PUT',
        params
    })
}

// 删除条目
export async function deleteItem(id) {
    return request(`/api/items/${id}`, {
        method: 'DELETE'
    })
}