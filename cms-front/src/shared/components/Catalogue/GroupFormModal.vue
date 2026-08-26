<!-- components/GroupFormModal.vue -->
<template>
    <a-modal
      v-model:open="props.visible"
      :title="isEdit ? '编辑分组' : '新增分组'"
      width="40%"
      :bodyStyle="{ padding: '10px' }"
      @ok="handleOk"
      @cancel="handleCancel"
    >
    <a-form ref="formRef" :model="form" :label-col="formLayoutInModal.labelCol" :wrapper-col="formLayoutInModal.wrapperCol" :rules="formRules">
      <a-form-item label="分组名称" name="title">
        <a-input v-model:value="form.title" placeholder="请输入分组名称,最多25个字符" />
      </a-form-item>
      <a-form-item label="排序" name="sort">
        <a-input-number v-model:value="form.sort" />
      </a-form-item>
    </a-form>
    </a-modal>
</template>

<script setup>
import { formLayoutInModal } from "@/consts/const";

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

const emit = defineEmits(['update:visible', 'ok']);
const formRef = ref();

const [form, setForm] = useState({
  title: "",
  sort: 0
});

const [formRules, setFormRules] = useState({
  title: [{ required: true, message: '请输入分组名称' }, { max: 25, message: '分组名称不能超过25个字符' }],
  sort: [{ required: true, message: '请输入排序' }]
});

watch(() => [props.visible, props.row, props.isEdit], () => {
  if (!props.visible) {
    return;
  }
  if (props.isEdit && props.row) {
    setForm({
      title: props.row.title,
      sort: props.row.sort ?? 0
    });
    return;
  }
  setForm({
    title: "",
    sort: 0
  });
}, { immediate: true });

const handleOk = () => {
  formRef.value.validate().then(() => {
    emit('ok', form.value);
    handleCancel();
  });
};

const handleCancel = () => {
  formRef.value?.resetFields();
  emit('update:visible', false);
};
</script>

<style scoped>

</style>