<template>
  <div class="docs-layout">
    <header class="docs-layout__header">
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
const docsSpace = import.meta.env.VITE_DOCS_SPACE || 'unknown'

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
  min-height: 100vh;
  background: var(--cms-color-bg);

  &__header {
    display: flex;
    align-items: center;
    height: var(--cms-header-height);
    padding: 0 24px;
    background: var(--cms-color-surface);
    border-bottom: 1px solid var(--cms-color-border);
    box-shadow: var(--cms-shadow-sm);
  }

  &__brand {
    display: flex;
    align-items: baseline;
    gap: 12px;
  }

  &__space {
    font-size: 18px;
    font-weight: 600;
    color: var(--cms-color-text);
  }

  &__subtitle {
    font-size: 13px;
    color: var(--cms-color-text-secondary);
  }

  &__main {
    flex: 1;
    min-height: 0;
    overflow: hidden;
  }
}
</style>
