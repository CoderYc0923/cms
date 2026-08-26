<template>
  <div class="admin-layout">
    <header class="admin-layout__header">
      <div class="admin-layout__brand">CMS</div>
      <nav class="admin-layout__nav">
        <router-link
          to="/spaces"
          class="admin-layout__nav-link"
          :class="{ 'is-active': isSpacesActive }"
        >
          空间管理
        </router-link>
        <span class="admin-layout__divider" />
        <router-link
          v-for="item in spaceLinks"
          :key="item.path"
          :to="item.path"
          class="admin-layout__space-link"
          :class="{ 'is-active': isActive(item.path) }"
        >
          {{ item.title }}
        </router-link>
      </nav>
      <div class="admin-layout__actions">
        <RightContent :top-menu="true" theme="light" />
      </div>
    </header>
    <main class="admin-layout__main">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import RightContent from '@/admin/components/GlobalHeader/RightContent.vue'
import { useSpacesStore } from '@/stores/spaces'

const route = useRoute()
const spacesStore = useSpacesStore()

const fallbackLinks = [
  { path: '/shopchup', title: 'Shopchup', slug: 'shopchup' },
  { path: '/iot', title: '物联网', slug: 'iot' }
]

const spaceLinks = computed(() => {
  const enabled = spacesStore.enabledList
  if (!enabled.length) {
    return fallbackLinks
  }
  return enabled.map(item => ({
    path: `/${item.slug}`,
    title: item.name,
    slug: item.slug
  }))
})

const isSpacesActive = computed(() => route.path === '/spaces' || route.path.startsWith('/spaces/'))

const isActive = path => route.path === path || route.path.startsWith(`${path}/`)

onMounted(async () => {
  try {
    await spacesStore.fetchList(1)
  } catch (error) {
    console.error('load spaces failed', error)
  }
})
</script>

<style scoped lang="less">
.admin-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
  background: var(--color-bg-page);
  color: var(--color-text-primary);
  font-size: var(--text-body);

  &__header {
    display: flex;
    align-items: center;
    gap: var(--workspace-gap);
    flex-shrink: 0;
    height: var(--header-height);
    padding: 0 24px;
    background: var(--color-bg-surface);
    border-bottom: 1px solid var(--color-border);
  }

  &__brand {
    flex-shrink: 0;
    font-size: var(--text-body);
    font-weight: 600;
    color: var(--color-text-primary);
  }

  &__nav {
    display: flex;
    align-items: center;
    gap: 4px;
    flex: 1;
    min-width: 0;
    overflow-x: auto;
  }

  &__divider {
    width: 1px;
    height: 14px;
    margin: 0 8px;
    background: var(--color-border);
    flex-shrink: 0;
  }

  &__nav-link,
  &__space-link {
    padding: 4px 8px;
    color: var(--color-text-secondary);
    font-size: var(--text-body);
    text-decoration: none;
    transition: color 0.15s ease;
    white-space: nowrap;

    &:hover {
      color: var(--color-text-primary);
    }

    &.is-active {
      color: var(--color-text-primary);
      font-weight: 500;
    }
  }

  &__actions {
    flex-shrink: 0;
    display: flex;
    align-items: center;
  }

  &__main {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    background: var(--color-bg-surface);
  }
}
</style>
