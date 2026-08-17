<template>
  <a-input placeholder="请输入" allowClear v-bind="options" />
</template>

<script setup>

const { options } = defineProps({
  options: {
    type: Object,
    default: () => ({})
  }
})
</script>

<style lang="less" scoped>

</style>
