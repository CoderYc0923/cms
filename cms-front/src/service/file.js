import request from '@/utils/request'

/**
 * 上传接口
 * @param {String} type 业务类型
 * @param {*} file FormData 或文件内容
 */
export async function importFile ({ type, accountId }, file) {
  const q = new URLSearchParams()
  if (type) q.set('businessType', type)
  if (accountId) q.set('accountId', accountId)
  const qs = q.toString()
  return request(`/api/admin/files/upload${qs ? `?${qs}` : ''}`, {
    headers: {
      'Content-Type': 'multipart/form-data'
    },
    method: 'POST',
    params: file
  })
}
