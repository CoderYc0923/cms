<template>
  <div class="user-menu">
    <a-dropdown v-if="userName" placement="bottomRight" :trigger="['click']">
      <button type="button" class="user-menu__trigger" aria-label="用户菜单">
        <span class="user-menu__avatar" :style="{ background: avatarColor }">
          {{ avatarInitial }}
        </span>
        <span class="user-menu__name">{{ userName }}</span>
        <DownOutlined class="user-menu__chevron" />
      </button>
      <template #overlay>
        <div class="user-menu__panel">
          <div class="user-menu__profile">
            <span class="user-menu__avatar user-menu__avatar--lg" :style="{ background: avatarColor }">
              {{ avatarInitial }}
            </span>
            <div class="user-menu__profile-text">
              <span class="user-menu__profile-name">{{ userName }}</span>
              <span class="user-menu__profile-hint">管理员</span>
            </div>
          </div>
          <div class="user-menu__divider" />
          <button type="button" class="user-menu__action" @click="handleLogout">
            <LogoutOutlined />
            <span>退出登录</span>
          </button>
        </div>
      </template>
    </a-dropdown>
    <button v-else type="button" class="user-menu__login" @click="handleGoLogin">
      登录
    </button>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { DownOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { useGlobalStore } from '@/stores/global'
import { useRoute, useRouter } from 'vue-router'

const AVATAR_PALETTE = ['#64748b', '#78716c', '#71717a', '#6b7280', '#737373', '#525252']

const global = useGlobalStore()
const user = useUserStore()
const router = useRouter()
const route = useRoute()
const userName = ref(JSON.parse(localStorage.getItem('userInfo') || 'null')?.username || '')

const avatarInitial = computed(() => {
  const name = userName.value?.trim()
  if (!name) {
    return '?'
  }
  return name.slice(0, 1).toUpperCase()
})

const avatarColor = computed(() => {
  const name = userName.value || ''
  let hash = 0
  for (let i = 0; i < name.length; i += 1) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash)
  }
  return AVATAR_PALETTE[Math.abs(hash) % AVATAR_PALETTE.length]
})

const updateUserName = () => {
  userName.value = JSON.parse(localStorage.getItem('userInfo') || 'null')?.username || ''
}

const handleLogout = () => {
  global.modal.confirm({
    title: '退出登录',
    content: '确定退出登录？',
    onOk: () =>
      user.loginOut().then(() => {
        updateUserName()
        router.replace({
          path: '/login',
          query: { redirect: route.fullPath }
        })
      }),
    onCancel() {}
  })
}

const handleGoLogin = () => {
  router.push({
    path: '/login',
    query: { redirect: route.fullPath }
  })
}
</script>

<style lang="less" scoped>
.user-menu {
  &__trigger {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 4px 8px 4px 4px;
    border: none;
    border-radius: var(--radius-md);
    background: transparent;
    cursor: pointer;
    transition: background 0.15s ease;

    &:hover {
      background: var(--color-bg-hover);
    }
  }

  &__avatar {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    width: 28px;
    height: 28px;
    border-radius: 50%;
    font-size: 12px;
    font-weight: 600;
    color: #fff;
    letter-spacing: 0.02em;

    &--lg {
      width: 36px;
      height: 36px;
      font-size: 14px;
    }
  }

  &__name {
    max-width: 120px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: var(--text-label);
    color: var(--color-text-primary);
  }

  &__chevron {
    font-size: 10px;
    color: var(--color-text-tertiary);
  }

  &__login {
    padding: 6px 14px;
    border: 1px solid var(--color-border);
    border-radius: var(--radius-md);
    background: var(--color-bg-surface);
    font-size: var(--text-label);
    color: var(--color-text-primary);
    cursor: pointer;
    transition: background 0.15s ease, border-color 0.15s ease;

    &:hover {
      background: var(--color-bg-hover);
      border-color: var(--color-text-tertiary);
    }
  }

  &__panel {
    min-width: 220px;
    padding: 8px;
    border-radius: var(--radius-md);
    background: var(--color-bg-elevated);
    box-shadow: var(--shadow-popup);
  }

  &__profile {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 8px;
  }

  &__profile-text {
    display: flex;
    flex-direction: column;
    gap: 2px;
    min-width: 0;
  }

  &__profile-name {
    font-size: var(--text-body);
    font-weight: 500;
    color: var(--color-text-primary);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__profile-hint {
    font-size: var(--text-caption);
    color: var(--color-text-secondary);
  }

  &__divider {
    height: 1px;
    margin: 4px 0;
    background: var(--color-border);
  }

  &__action {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    padding: 8px 10px;
    border: none;
    border-radius: var(--radius-sm);
    background: transparent;
    font-size: var(--text-label);
    color: var(--color-text-primary);
    cursor: pointer;
    transition: background 0.15s ease;

    &:hover {
      background: var(--color-bg-hover);
    }
  }
}
</style>
