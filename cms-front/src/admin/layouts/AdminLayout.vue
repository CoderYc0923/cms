<template>
  <div class="admin-layout">
    <header class="admin-layout__header">
      <div class="admin-layout__brand">CMS Admin</div>
      <nav class="admin-layout__spaces">
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
import RightContent from '@/components/GlobalHeader/RightContent.vue'

const route = useRoute()

const spaceLinks = [
  { path: '/shopchup', title: 'Shopchup' },
  { path: '/iot', title: '物联网' }
]

const isActive = path => route.path === path || route.path.startsWith(`${path}/`)
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

  &__spaces {
    display: flex;
    align-items: center;
    gap: 8px;
    flex: 1;
    min-width: 0;
  }

  &__space-link {
    padding: 6px 14px;
    border-radius: 999px;
    color: var(--cms-color-text-secondary);
    text-decoration: none;
    transition: all 0.2s ease;

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
