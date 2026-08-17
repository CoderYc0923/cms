import { watchEffect } from 'vue'
import { useUserStore } from '@/stores/user'
/* 
<!-- 已登录才显示（默认） -->
<a-button v-auth type="primary" @click="handleAddGroup">新增分组</a-button>

<!-- 未登录才显示，例如登录入口 -->
<a v-auth="false" @click="handleOpenLoginModal">立即登录</a>

<!-- 可与 v-if 等业务条件组合 -->
<a-button v-auth v-if="isEdit" @click="handleAddGroup">新增分组</a-button> */

function setVisible (el, visible) {
  if (visible) {
    el.style.display = el._authOriginalDisplay || ''
    el.removeAttribute('hidden')
  } else {
    el.style.display = 'none'
  }
}

function checkAuth (el, binding, isLoggedIn) {
  // 默认 true：已登录才显示；传 false 时未登录才显示（如「立即登录」）
  const needLogin = binding.value !== false
  setVisible(el, needLogin ? isLoggedIn : !isLoggedIn)
}

export const auth = {
  mounted (el, binding) {
    el._authOriginalDisplay = el.style.display || getComputedStyle(el).display
    const userStore = useUserStore()
    el._stopAuthWatch = watchEffect(() => {
      checkAuth(el, binding, userStore.isLoggedIn)
    })
  },
  updated (el, binding) {
    const userStore = useUserStore()
    checkAuth(el, binding, userStore.isLoggedIn)
  },
  unmounted (el) {
    el._stopAuthWatch?.()
  }
}
