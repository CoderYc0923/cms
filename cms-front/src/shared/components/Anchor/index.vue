<template>
  <div class="cms-anchor">
    <a-anchor
      v-if="items.length"
      :affix="false"
      :items="items"
      :get-container="resolveContainer"
      @click="handleClick"
    />
    <div v-else class="cms-anchor__empty">暂无目录</div>
  </div>
</template>

<script setup>
const props = defineProps({
  anchorItems: {
    type: Array,
    default: () => []
  },
  container: {
    type: Function,
    default: () => window
  },
  /** 滚动到标题时，距离容器顶部的留白（px） */
  scrollOffset: {
    type: Number,
    default: 24
  }
})

const [items, setItems] = useState([])

const resolveContainer = () => {
  const target = props.container?.()
  if (target && target.nodeType === 1) {
    return target
  }
  return window
}

const scrollToHeading = (targetId) => {
  const scrollContainer = resolveContainer()
  const selector = `#${CSS.escape(targetId)}`
  const targetElement = scrollContainer?.querySelector?.(selector)
    || document.getElementById(targetId)

  if (!targetElement) {
    return
  }

  if (scrollContainer === window) {
    const top = window.scrollY + targetElement.getBoundingClientRect().top - props.scrollOffset
    window.scrollTo({ top, behavior: 'smooth' })
    return
  }

  const containerRect = scrollContainer.getBoundingClientRect()
  const targetRect = targetElement.getBoundingClientRect()
  const nextTop = scrollContainer.scrollTop + (targetRect.top - containerRect.top) - props.scrollOffset

  scrollContainer.scrollTo({
    top: Math.max(nextTop, 0),
    behavior: 'smooth'
  })
}

const handleClick = (e, link) => {
  e.preventDefault()
  const href = link?.href || ''
  const targetId = href.startsWith('#') ? href.slice(1) : href.replace(/^#/, '')
  if (targetId) {
    scrollToHeading(targetId)
  }
}

watch(
  () => props.anchorItems,
  anchorItems => {
    setItems(anchorItems || [])
  },
  { immediate: true }
)
</script>

<style lang="less" scoped>
.cms-anchor {
  &__empty {
    padding: 8px 12px;
    font-size: var(--text-caption);
    color: var(--color-text-tertiary);
    line-height: 20px;
  }

  :deep(.ant-anchor-wrapper) {
    margin: 0;
    padding: 0;
    max-height: none;
  }

  :deep(.ant-anchor) {
    padding-inline-start: 0;

    &::before {
      display: none;
    }
  }

  :deep(.ant-anchor-ink) {
    display: none !important;
  }

  :deep(.ant-anchor-link) {
    padding: 0;
    margin: 0 0 2px;
  }

  :deep(.ant-anchor-link-title) {
    display: block;
    padding: 6px 10px;
    border-radius: var(--radius-sm);
    font-size: var(--text-caption);
    line-height: 20px;
    color: var(--color-text-secondary);
    transition: color 0.15s ease, background 0.15s ease;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.ant-anchor-link-title:hover) {
    color: var(--color-text-primary);
    background: var(--color-border-subtle);
  }

  :deep(.ant-anchor-link-active > .ant-anchor-link-title) {
    color: var(--color-text-primary);
    font-weight: 500;
    background: var(--color-bg-hover);
  }

  /* 二级及以下标题缩进 */
  :deep(.ant-anchor-link .ant-anchor-link) {
    padding-inline-start: 12px;
  }

  :deep(.ant-anchor-link .ant-anchor-link .ant-anchor-link) {
    padding-inline-start: 24px;
  }
}
</style>
