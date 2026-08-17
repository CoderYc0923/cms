<!-- 节流按钮 -->
<template>
  <a-button @click="handleClick" v-bind="attrs">
    <slot></slot>
  </a-button>
</template>

<script setup>
import { useAttrs } from 'vue'
import { useThrottleFn } from '@vueuse/core'

const attrs = useAttrs()
const emits = defineEmits(['click'])

const handleClick = useThrottleFn(() => {
  emits('click')
}, 1000)
</script>

<style lang="less" scoped>

</style>
