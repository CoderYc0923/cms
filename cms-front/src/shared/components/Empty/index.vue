<template>
  <div class="empty">
    <a-empty :image="simpleImage"/>
  </div>
</template>

<script setup>
import { Empty } from 'ant-design-vue';

const simpleImage = Empty.PRESENTED_IMAGE_SIMPLE;
</script>

<style lang="less" scoped>
.empty {
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
}
</style>