<!-- eslint-disable vue/no-mutating-props -->
<template>
  <a-space>
    <a-select v-model:value="selectedMode" @change="handleModeChange">
      <a-select-option
        v-for="(mode, index) in MODE_TYPE"
        :key="index"
        :value="mode"
        >{{ MODE_TYPE_MSG[mode] }}</a-select-option
      >
    </a-select>
    <!-- <a-range-picker
      :picker="selectedMode"
      :placeholder="options.placeholder"
      :locale="locale"
      :showTime="
        options.showTime
          ? Object.assign(
              {
                defaultValue: [
                  dayjs('00:00:00', 'HH:mm:ss'),
                  dayjs('23:59:59', 'HH:mm:ss'),
                ],
              },
              options.showTime
            )
          : false
      "
      @change="pickerChange"
      v-model:value="props.value"
      :disabled-date="options.disabledDate"
    /> -->
  </a-space>
</template>

<script setup>
import dayjs from "dayjs";
import locale from "ant-design-vue/es/date-picker/locale/zh_CN";

const MODE_TYPE = {
  DATE: "date",
  MONTH: "month",
};

const MODE_TYPE_MSG = {
  [MODE_TYPE.DATE]: "按天",
  [MODE_TYPE.MONTH]: "按月",
};

// 属性
const props = defineProps({
  options: {
    type: Object,
    default: () => ({}),
  },
  value: {
    type: Array,
    default: () => [],
  },
});
// 组件绑定事件
const emit = defineEmits(["update:value"]);

const selectedMode = ref(MODE_TYPE.DATE);

const pickerChange = (value) => {
  emit("update:value", value);
};

const handleModeChange = (value) => {
  selectedMode.value = value;
};
</script>

<style lang="less" scoped>
.ant-picker {
  width: 100%;
}
</style>
