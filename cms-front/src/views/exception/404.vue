<template>
  <a-result status="404" title="404" sub-title="对不起，您访问的页面不存在。">
    <template #extra>
      <a-button type="primary" @click="() => backToHome()">返回首页</a-button>
    </template>
  </a-result>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { defaultSetting } from '@/config/defaultSetting'

const router = useRouter()

const backToHome = () => {
  router.push(defaultSetting.defaultRoute)
}
</script>

<style scoped>

</style>
