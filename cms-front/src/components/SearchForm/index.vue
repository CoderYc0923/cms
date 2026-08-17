<template>
  <div class="__search_form">
    <a-form
      :model="form"
      ref="proTableFormRef"
      layout="inline"
      :label-col="{ span: labelColSpan }"
      :wrapper-col="{ span: wrapperColSpan }"
      autocomplete="off"
      hideRequiredMark
      class="form"
    >
      <a-row class="row" :gutter="[0, 24]">
        <template
          v-for="controls in props.columns.filter((s) => !s.hideInSearch)"
          :key="controls.key"
        >
          <a-col
            :xl="{ span: controls?.custom?.mode === 'SWITCH_MODE_PICKER' ? 11 : 8 }"
            :xxl="{ span: controls?.custom?.mode === 'SWITCH_MODE_PICKER' ? 8 : 8 }"
            :sm="{ span: 12 }"
            :xs="{ span: 24 }"
          >
            <a-form-item
              :label="controls.title"
              :name="controls.key"
              :rules="controls.custom?.rules"
            >
              <template v-if="controls.valueType === MODE_TYPE.DATE_PICKER">
                <component
                  :is="modeToControls[controls.valueType]"
                  :options="controls.custom"
                  @modeChange="modeChange"
                  v-model:value="form[controls.key]"
                />
              </template>
              <component
                v-else
                :is="
                  !controls.valueType
                    ? modeToControls.INPUT
                    : modeToControls[controls.valueType]
                "
                :options="controls.custom"
                v-model:value.trim="form[controls.key]"
                @modeChange="modeChange"
              />
            </a-form-item>
          </a-col>
        </template>
        <a-col
          :xxl="{ span: 6 }"
          :xl="{ span: 8 }"
          :sm="{ span: 12 }"
          :xs="{ span: 24 }"
          style="margin-left: auto"
        >
          <a-form-item
            style="white-space: nowrap; text-align: right"
            v-bind="{ wrapperCol: { span: 24 } }"
          >
            <ThrottleButton style="margin-right: 8px" @click="resetSearchForm"
              >重置</ThrottleButton
            >
            <ThrottleButton
              type="primary"
              @click="handleSearch"
              :loading="loading"
            >
              <SearchOutlined />
              搜索
            </ThrottleButton>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </div>
</template>

<script setup>
import ThrottleButton from "@/components/Button/ThrottleButton.vue";

import { SearchOutlined } from "@ant-design/icons-vue";
import {
  InputFilter,
  SelectFilter,
  DateFilter,
} from "./filter";
import { ref, toRaw, onMounted } from "vue";
import { cloneDeep } from "lodash";

const MODE_TYPE = {
  INPUT: "INPUT",
  SELECT: "SELECT",
  DATE_PICKER: "DATE_PICKER",
};

const modeToControls = {
  [MODE_TYPE.INPUT]: InputFilter,
  [MODE_TYPE.SELECT]: SelectFilter,
  [MODE_TYPE.DATE_PICKER]: DateFilter,
};

const proTableFormRef = ref();
const emit = defineEmits(["request", "formChange", "modeChange"]);
const props = defineProps({
  columns: {
    type: Array,
    default: () => [],
  },
  loading: {
    type: Boolean,
    default: () => false,
  },
  labelColSpan: {
    type: Number,
    default: 6,
  },
  wrapperColSpan: {
    type: Number,
    default: 17,
  },
});

// 获取表单初始值
const [formDefaultValue] = useState({});
props.columns.forEach((item) => {
  if (typeof item.defaultValue === "undefined") return;
  formDefaultValue.value[item.key] = item.defaultValue;
});

let [form, setForm] = useState(formDefaultValue.value);

// const pickerChange = (key, value) => {
//   form.value = cloneDeep({ ...form.value, [key]: value })
// }

// 搜索 表单提交
const handleSearch = (params) => {
  proTableFormRef.value
    .validate()
    .then(() => {
      emit("request", toRaw(form.value), { reset: true, ...params });
    })
    .catch((error) => {
      console.error(error);
    });
};
// 重置
const resetSearchForm = () => {
  proTableFormRef.value.resetFields();
  setForm(formDefaultValue.value);

  emit("request", toRaw(form.value), { reset: true });
};

const modeChange = (mode) => {
  emit('modeChange', mode)
}

// 监听form的值变化
watch(
  form.value,
  () => {
    emit("formChange", toRaw(form.value));
  },
  {
    deep: true,
  }
);

onMounted(() => {
  // 首次自动执行
  handleSearch();
});

defineExpose({
  handleSearch,
  resetSearchForm,
});
</script>

<style lang="less" scoped>
.form .row {
  width: 100%;
}
:where(.css-dev-only-do-not-override-hkh161).ant-form-inline .ant-form-item {
  margin-inline-end: 0px;
}
:deep(:where(.css-dev-only-do-not-override-hkh161).ant-col) {
  max-width: 100% !important;
}
</style>
