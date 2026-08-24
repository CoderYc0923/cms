const PUBLIC_FILE_URL_PATTERN = /\/api\/public\/files\/(\d+)\/content/g

/** Admin 编辑/预览时，private 文件需走 admin 302 接口 */
export function rewriteAdminFileUrls (html) {
  if (!html) {
    return html
  }
  return html.replace(PUBLIC_FILE_URL_PATTERN, '/api/admin/files/$1/content')
}

/** 存库/发布用 stableUrl（public 路径） */
export function toPublicFileUrl (html) {
  if (!html) {
    return html
  }
  return html.replace(/\/api\/admin\/files\/(\d+)\/content/g, '/api/public/files/$1/content')
}
