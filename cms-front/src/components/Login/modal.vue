<!-- components/GroupFormModal.vue -->
<template>
    <a-modal
      v-model:open="props.visible"
      title="登录"
      width="40%"
      :bodyStyle="{ padding: '0' }"
      @ok="handleOk"
      @cancel="handleCancel"
    >
    <a-form ref="formRef" :model="form" :label-col="formLayoutInModal.labelCol" :wrapper-col="formLayoutInModal.wrapperCol" :rules="formRules">
      <a-form-item label="用户名" name="username">
        <a-input v-model:value="form.username" placeholder="请输入用户名" />
      </a-form-item>
      <a-form-item label="密码" name="password">
        <a-input-password v-model:value="form.password" placeholder="请输入密码" />
      </a-form-item>
    </a-form>
    </a-modal>
</template>

<script setup>
import { formLayoutInModal } from "@/consts/const";
import { useUserStore } from "@/stores/user";
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
});


const emit = defineEmits(['update:visible', 'ok']);
const formRef = ref();
const user = useUserStore();
const [form, setForm] = useState({
  username: "",
  password: ""
});

const [formRules, setFormRules] = useState({
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }]
});

const handleOk = () => {
  formRef.value.validate().then(() => {
    handleLogin(form.value);
  });
};

const handleLogin = (form) => {
  user.login(form).then(() => {
    emit('update:visible', false);
    emit('ok');
  })
};

const handleCancel = () => {
  formRef.value?.resetFields();
  emit('update:visible', false);
};
</script>

<style scoped>

</style>