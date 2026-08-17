<template>
  <a-select
    placeholder="请选择"
    :mode="options.mode"
    :maxTagCount="options.maxTagCount"
    optionFilterProp="text"
    :showSearch="!!options.list"
    v-bind="options"
  >
    <a-select-option v-if="options.showAll" :value="null" searchItem="">全部</a-select-option>
    <!-- 非枚举类型 -->
    <template v-if="options.list">
      <a-select-option
        v-for="item in options.list.dataSource"
        :key="item[options.list.key]"
        :value="item[options.list.key]"
        :searchItem="item[options.list.value]"
      >
        <my-icon v-if="item.icon" :type="item.icon" />
        {{ item[options.list.value] }}
      </a-select-option>
    </template>
    <!-- 枚举类型 -->
    <template v-if="options.enum">
      <a-select-option v-for="key in Object.keys(options.enum)" :key="key" :value="key">
        {{ options.enum[key].text || options.enum[key] }}
      </a-select-option>
    </template>
  </a-select>
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
