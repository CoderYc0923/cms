<template>
  <div>
    <a-dropdown v-if="userName" placement="bottomRight">
      <span class="ant-pro-account-avatar">
        <a-avatar
          size="small"
          src="/img/default_avatar.png"
          class="antd-pro-global-header-index-avatar"
        />
        <span class="ant-pro-account-avatar_name">{{ userName }}</span>
      </span>
      <template v-slot:overlay>
        <a-menu class="ant-pro-drop-down menu">
          <a-menu-item key="logout" @click="handleLogout">
            <ArrowLeftOutlined />退出登录
          </a-menu-item>
        </a-menu>
      </template>
    </a-dropdown>
    <span v-else>
      <a @click="handleOpenLoginModal">立即登录</a>
    </span>
    <LoginModal :visible="loginVisible" @update:visible="setLoginVisible" @ok="handleLogin" />
  </div>
</template>

<script setup>
import { ref } from "vue";
import { ArrowLeftOutlined } from "@ant-design/icons-vue";
import { useUserStore } from "@/stores/user";
import { useRouter } from "vue-router";
import LoginModal from "@/components/Login/modal.vue";

const global = useGlobalStore();
const user = useUserStore();
const router = useRouter();
const userName = ref(JSON.parse(localStorage.getItem("userInfo"))?.username || "");

const [loginVisible, setLoginVisible] = useState(false);

const updateUserName = () => {
  userName.value = JSON.parse(localStorage.getItem("userInfo"))?.username || "";
};

const handleLogout = () => {
  global.modal.confirm({
    title: "退出登录",
    content: "确定退出登录？",
    onOk: () =>
      user.loginOut().then(() => {
        updateUserName();
      }),
    onCancel() {}
  });
};

const handleOpenLoginModal = () => {
  setLoginVisible(true);
};

const handleLogin = () => {
  updateUserName();
};
</script>

<style lang="less" scoped>
.ant-pro-account-avatar {
  &_name {
    margin-left: 8px;
  }
}
.ant-pro-drop-down {
  :deep(.action) {
    margin-right: 8px;
  }
  :deep(.ant-dropdown-menu-item) {
    min-width: 100px;
  }
}
</style>
