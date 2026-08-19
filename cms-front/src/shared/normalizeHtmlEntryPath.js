/** MPA 入口为 *.html 时，把地址栏规范为 /，避免 Vue Router 匹配到 /admin.html 导致 404 */
export function normalizeHtmlEntryPath () {
  const { pathname, search, hash } = window.location
  if (!/\.html$/i.test(pathname)) {
    return
  }
  const nextPath = pathname.replace(/[^/]+\.html$/i, '') || '/'
  window.history.replaceState(null, '', `${nextPath}${search}${hash}`)
}
