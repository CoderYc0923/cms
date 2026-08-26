<template>
  <div class="admin-layout">
    <header class="admin-layout__header">
      <div class="admin-layout__brand">CMS Admin</div>
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
      <div class="admin-layout__content">
        <router-view />
      </div>
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
  background: var(--cms-color-bg);

  &__header {
    display: flex;
    align-items: center;
    gap: 24px;
    flex-shrink: 0;
    height: var(--cms-header-height);
    padding: 0 24px;
    background: var(--cms-color-surface);
    border-bottom: 1px solid var(--cms-color-border);
    box-shadow: var(--cms-shadow-sm);
  }

  &__brand {
    flex-shrink: 0;
    font-size: 16px;
    font-weight: 600;
    color: var(--cms-color-text);
  }

  &__nav {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 0;
    overflow-x: auto;
  }

  &__divider {
    width: 1px;
    height: 16px;
    margin: 0 4px;
    background: var(--cms-color-border);
    flex-shrink: 0;
  }

  &__nav-link,
  &__space-link {
    padding: 6px 14px;
    border-radius: 999px;
    color: var(--cms-color-text-secondary);
    text-decoration: none;
    transition: all 0.2s ease;
    white-space: nowrap;

    &:hover {
      color: var(--cms-color-text);
      background: rgba(18, 18, 63, 0.04);
    }

    &.is-active {
      color: #1677ff;
      background: rgba(22, 119, 255, 0.08);
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
    padding: 16px 24px 24px;
    overflow: hidden;
  }

  &__content {
    height: 100%;
    background: #fff;
    border-radius: var(--cms-radius-md);
    box-shadow: var(--cms-shadow-sm);
    overflow: hidden;
  }
}
</style>
