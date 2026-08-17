<!-- components/GroupFormModal.vue -->
<template>
  <a-modal
    v-model:open="props.visible"
    :title="isEdit ? '编辑项' : '新增项'"
    width="40%"
    :bodyStyle="{ padding: '10px' }"
    @ok="handleOk"
    @cancel="handleCancel"
  >
    <a-form
      ref="formRef"
      :model="form"
      :label-col="formLayoutInModal.labelCol"
      :wrapper-col="formLayoutInModal.wrapperCol"
      :rules="formRules"
    >
      <a-form-item label="标题" name="title">
        <a-input v-model:value="form.title" placeholder="请输入标题,最多10个字符" />
      </a-form-item>
      <a-form-item label="类型" name="type">
        <a-radio-group v-model:value="form.type">
          <a-radio
            :value="item"
            v-for="item in Object.values(MENU_TYPE).filter(item => item !== MENU_TYPE.GROUP)"
            :key="item"
          >{{ MENU_TYPE_MSG[item] }}</a-radio>
        </a-radio-group>
      </a-form-item>
      <a-form-item label="排序" name="sort">
        <a-input-number v-model:value="form.sort" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { formLayoutInModal } from "@/consts/const";
import { MENU_TYPE, MENU_TYPE_MSG } from "@/consts/enum";

const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  isEdit: {
    type: Boolean,
    default: false
  },
  row: {
    type: Object,
    default: () => null
  }
});

const emit = defineEmits(["update:visible", "ok"]);
const formRef = ref();

const [form, setForm] = useState({
  title: "",
  type: MENU_TYPE.MENU,
  sort: 0
});

const [formRules, setFormRules] = useState({
  title: [
    { required: true, message: "请输入名称" },
    { max: 50, message: "名称不能超过50个字符" }
  ],
  type: [{ required: true, message: "请选择类型" }],
  sort: [{ required: true, message: "请输入排序" }]
});

watch(
  () => props.row,
  newVal => {
    if (newVal && props.isEdit) {
      setForm({
        title: newVal.title,
        type: newVal.type,
        sort: newVal.sort
      });
    }
  },
  { immediate: true }
);

const handleOk = () => {
  formRef.value.validate().then(() => {
    emit("ok", form.value);
    handleCancel();
  });
};

const handleCancel = () => {
  formRef.value?.resetFields();
  emit("update:visible", false);
};
</script>

<style scoped>
</style>