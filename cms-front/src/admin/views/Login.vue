<template>
  <div class="admin-login">
    <div class="admin-login__card">
      <h1 class="admin-login__title">CMS Admin</h1>
      <p class="admin-login__subtitle">登录后管理文档空间</p>
      <a-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        layout="vertical"
        @finish="handleSubmit"
      >
        <a-form-item label="用户名" name="username">
          <a-input v-model:value="form.username" placeholder="请输入用户名" size="large" />
        </a-form-item>
        <a-form-item label="密码" name="password">
          <a-input-password v-model:value="form.password" placeholder="请输入密码" size="large" />
        </a-form-item>
        <a-button type="primary" html-type="submit" block size="large" :loading="submitting">
          登录
        </a-button>
      </a-form>
    </div>
  </div>
</template>

<script setup>
import { message } from 'ant-design-vue'
import { useUserStore } from '@/stores/user'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const formRef = ref()
const submitting = ref(false)

const form = reactive({
  username: '',
  password: ''
})

const formRules = {
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }]
}

const resolveRedirect = () => {
  const redirect = route.query.redirect
  if (typeof redirect === 'string' && redirect.startsWith('/') && !redirect.startsWith('/login')) {
    return redirect
  }
  return '/shopchup'
}

const handleSubmit = async () => {
  submitting.value = true
  try {
    await userStore.login({
      username: form.username,
      password: form.password
    })
    message.success('登录成功')
    await router.replace(resolveRedirect())
  } catch (error) {
    message.error(error?.message || error?.msg || '登录失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped lang="less">
.admin-login {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: var(--color-bg-page);

  &__card {
    width: 400px;
    padding: 40px 36px 32px;
    background: var(--color-bg-surface);
    border: 1px solid var(--color-border);
    border-radius: var(--radius-lg);
    box-shadow: var(--shadow-popup);
  }

  &__title {
    margin: 0;
    font-size: var(--text-title-lg);
    font-weight: 600;
    color: var(--color-text-primary);
    text-align: center;
  }

  &__subtitle {
    margin: 8px 0 28px;
    font-size: var(--text-body);
    color: var(--color-text-secondary);
    text-align: center;
  }
}
</style>
