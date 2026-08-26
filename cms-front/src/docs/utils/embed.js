const CMS_DOCS_ORIGIN = 'cms-docs'

export function isEmbedMode () {
  if (import.meta.env.VITE_DOCS_EMBED === 'true') {
    return true
  }

  const params = new URLSearchParams(window.location.search)
  if (params.get('embed') === '1' || params.get('embed') === 'true') {
    return true
  }

  try {
    return window.self !== window.top
  } catch {
    return true
  }
}

/** 自适应高度 embed：向父页 postMessage 报告内容高度 */
export function isAutoHeightEmbed () {
  if (!isEmbedMode()) {
    return false
  }

  const params = new URLSearchParams(window.location.search)
  return params.get('embed') === 'auto' || import.meta.env.VITE_DOCS_EMBED_AUTO === 'true'
}

function postToParent (payload) {
  if (!isEmbedMode() || window.parent === window.self) {
    return
  }

  window.parent.postMessage(
    {
      source: CMS_DOCS_ORIGIN,
      ...payload
    },
    '*'
  )
}

function postHeight () {
  const height = Math.max(
    document.documentElement.scrollHeight,
    document.body?.scrollHeight || 0
  )
  postToParent({ type: 'height', height })
}

export function setupEmbedBridge (router) {
  if (!isEmbedMode()) {
    return () => {}
  }

  document.documentElement.classList.add('cms-docs-embed')

  if (isAutoHeightEmbed()) {
    document.documentElement.classList.add('cms-docs-embed--auto')
  }

  const resizeObserver = new ResizeObserver(() => {
    postHeight()
  })
  resizeObserver.observe(document.documentElement)

  const onMessage = event => {
    const data = event.data
    if (!data || data.source !== CMS_DOCS_ORIGIN || data.type !== 'navigate') {
      return
    }

    if (typeof data.path === 'string' && data.path !== router.currentRoute.value.fullPath) {
      router.push(data.path).catch(() => {})
    }

    if (data.nodeId != null) {
      const nodeId = Number(data.nodeId)
      if (Number.isFinite(nodeId)) {
        router.replace({
          name: 'DocsArticle',
          params: { nodeId }
        }).catch(() => {})
      }
    }
  }

  window.addEventListener('message', onMessage)

  const removeAfterEach = router.afterEach(to => {
    postToParent({
      type: 'route',
      path: to.fullPath,
      nodeId: to.params.nodeId ?? null
    })
    postHeight()
  })

  postToParent({ type: 'ready' })
  postHeight()

  return () => {
    resizeObserver.disconnect()
    window.removeEventListener('message', onMessage)
    removeAfterEach()
  }
}
