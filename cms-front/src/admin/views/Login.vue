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
  background: linear-gradient(160deg, #f5f7fb 0%, #eef2ff 100%);

  &__card {
    width: 400px;
    padding: 40px 36px 32px;
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 12px 40px rgba(18, 18, 63, 0.08);
  }

  &__title {
    margin: 0;
    font-size: 24px;
    font-weight: 600;
    color: #12123f;
    text-align: center;
  }

  &__subtitle {
    margin: 8px 0 28px;
    color: rgba(0, 0, 0, 0.45);
    text-align: center;
  }
}
</style>
