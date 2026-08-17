<template>
  <icon-font style="font-size: 16px;" v-if="props.type" :type="props.type" />
</template>
<script setup>
import { createFromIconfontCN } from '@ant-design/icons-vue'

const props = defineProps({
  type: {
    type: String,
    required: true
  }
})

const IconFont = createFromIconfontCN({
  scriptUrl: '/iconfont.js' // 在 iconfont.cn 上生成, 2025-03-10：改为从本地加载，以防发生加载失败问题，更新icon时需替换iconfont.js的文件内容
})
</script>
<style scoped>
</style>
