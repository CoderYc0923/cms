<template>
  <div class="docs-layout docs-theme" :class="{ 'docs-layout--embed': embedMode }">
    <header v-if="!embedMode" class="docs-layout__header">
      <div class="docs-layout__brand">
        <span class="docs-layout__space">{{ spaceTitle }}</span>
        <span class="docs-layout__subtitle">文档中心</span>
      </div>
    </header>
    <main class="docs-layout__main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { isEmbedMode } from '@/docs/utils/embed'

const docsSpace = import.meta.env.VITE_DOCS_SPACE || 'unknown'
const embedMode = isEmbedMode()

const spaceTitleMap = {
  iot: '物联网',
  shopchup: 'Shopchup'
}

const spaceTitle = computed(() => spaceTitleMap[docsSpace] || docsSpace)
</script>

<style scoped lang="less">
.docs-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--color-bg-page);
  color: var(--color-text-primary);

  &__header {
    position: sticky;
    top: 0;
    z-index: 10;
    display: flex;
    align-items: center;
    height: var(--header-height);
    padding: 0 24px;
    background: var(--color-bg-surface);
    border-bottom: 1px solid var(--color-border);
  }

  &__brand {
    display: flex;
    align-items: baseline;
    gap: 12px;
  }

  &__space {
    font-size: var(--text-title-md);
    font-weight: 600;
    color: var(--color-text-primary);
  }

  &__subtitle {
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
  }

  &__main {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }

  &--embed {
    height: 100%;
    min-height: 100vh;
  }
}
</style>

<style lang="less">
html.cms-docs-embed,
html.cms-docs-embed body,
html.cms-docs-embed #app {
  height: 100%;
}

html.cms-docs-embed--auto,
html.cms-docs-embed--auto body,
html.cms-docs-embed--auto #app {
  height: auto;
  min-height: 100%;
}

html.cms-docs-embed--auto .docs-layout--embed {
  height: auto;
  min-height: 100vh;
  overflow: visible;
}

html.cms-docs-embed--auto .docs-viewer {
  height: auto;
  min-height: 100vh;
}

html.cms-docs-embed--auto .docs-viewer__content,
html.cms-docs-embed--auto .docs-viewer__sidebar {
  height: auto;
  min-height: 100vh;
}
</style>
