<template>
  <a-config-provider
    :locale="locale"
    :theme="myTheme"
  >
    <a-app>
      <router-view/>
    </a-app>
  </a-config-provider>
</template>

<script setup>

import zhCN from 'ant-design-vue/es/locale/zh_CN'
import dayjs from 'dayjs'
import 'dayjs/locale/zh-cn'

dayjs.locale(zhCN)
const locale = zhCN

const myTheme = {
  token: {
    borderRadius: 4
  }
}

</script>

<style scoped>
</style>
