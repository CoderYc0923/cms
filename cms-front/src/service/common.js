import request from '@/utils/request'

// 获取文件下载地址
export async function downloadFile (params) {
  return request('/file/get', { method: 'GET', params })
}

/* **************** 根据path获取图片 *************** */
export async function getPreviewImg (params) {
  return request('/file/preview', {
    method: 'GET',
    params
  })
}

// 获取目录树
export async function getDirectoryTree (source) {
  return request('/api/public/content', {
    method: 'GET',
    params: {
      source
    }
  })
}
