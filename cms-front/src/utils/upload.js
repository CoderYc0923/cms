import { imageToBlob, getBase64 } from '@/utils/util'

export function handleImgChange (info, type, obj) {
  getBase64(info.file.originFileObj, imageUrl => {
    obj[type] = imageUrl
  })
}
// 获取上传文件判断文件格式 返回布尔值
export function uploadFile (file, type, obj) {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/jpg'
  if (!isJpgOrPng) {
    this.$message.error('上传图片仅支持png、jpg、jpeg格式!')
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    this.$message.error('上传图片大小在5M内!')
  }
  if (isJpgOrPng && isLt5M) {
    obj[type] = file
  }
  return isJpgOrPng && isLt5M
}

// 图片上传前的转换
export function handleBeforeUpload (url) {
  return turnImageToFile(url)
}

async function turnImageToFile (imageUrl) {
  if (!imageUrl || !imageUrl.length) return null
  const urlPart = imageUrl.split('/')
  const fileName = urlPart[ urlPart.length - 1 ].split('?')[ 0 ] || '*'
  const fileType = 'image/' + (fileName.split('.')[ 1 ])
  const blob = await (new Promise((resolve, reject) => {
    imageToBlob(imageUrl, resolve, err => {
      reject(err)
    })
  }).catch(error => {
    console.error(error)
  }))
  return new File([ blob ], fileName, { type: fileType })
}
