<template>
  <div class="rightContent">
    <avatar-dropdown />
  </div>
</template>

<script setup>
import AvatarDropdown from './AvatarDropdown.vue'

defineProps({
  isMobile: {
    type: Boolean,
    default: false
  },
  topMenu: {
    type: Boolean,
    required: true
  },
  theme: {
    type: String,
    required: true
  }
})

</script>
<style lang="less" scoped>
.rightContent {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: nowrap;
  height: auto;
  padding-right: 0;
}
</style>