<template>
  <div>
    选择平台：
    <a-tag
      v-for="(item, key) in PLATFORM_MAP_MSG"
      :key="key"
      :color="+curPlatform === +key ? '#6666CC' : ''"
      style="cursor: pointer;"
      @click="handleSwitchPlatform(key)"
    >
      {{ item.name }}
    </a-tag>
    <!-- <a-tag color="#6666CC">{{ PLATFORM_MAP_MSG[curPlatform].name }}平台</a-tag> -->
  </div>
</template>

<script>
import { PLATFORM_MAP, PLATFORM_MAP_MSG } from '@/consts/platform'
export default {
  data () {
    return {
      PLATFORM_MAP_MSG,
      curPlatform: PLATFORM_MAP.WC
    }
  },
  methods: {
    handleSwitchPlatform (key) {
      // 先关闭选择平台功能
      if (key !== this.curPlatform) {
        this.curPlatform = key
        localStorage.setItem('platform', key)
        this.$message.loading({ content: `即将切换到【${PLATFORM_MAP_MSG[key].name}】平台...`, key: 'updatable' })
        setTimeout(() => {
          location.replace('/')
        }, 1000)
      }
    }
  },
  mounted () {
    this.curPlatform = localStorage.getItem('platform') || PLATFORM_MAP.CN
  }
}
</script>
