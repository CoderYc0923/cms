<template>
  <pro-layout
    :locale="locale"
    v-bind="layoutConf"
    v-model:collapsed="state.collapsed"
    :selectedKeys="state.selectedKeys"
    :openKeys="state.openKeys"
    :menuData="menuData"
    @collapse="(value) => (state.collapsed = value)"
  >
    <!-- logo -->
    <!-- <template v-slot:menuHeaderRender>
      <img v-if="!state.collapsed" src="@/assets/shopchup_logo.png" style="width: 126px;height: 26px;">
      <img v-else src="/logo.svg" style="width: 126px;height: 26px;">
    </template>
    -->
    <!-- 头部右侧 -->
    <template v-slot:rightContentRender>
      <RightContent :top-menu="true" :theme="layoutConf.navTheme" />
    </template>

    <!-- <page-container size="small" :title="title">
      <a-breadcrumb v-if="route.meta.needBack" style="margin-bottom: 16px">
        <a-breadcrumb-item @click="handleBack" style="cursor:pointer">
          <my-icon type="icon-n-back" /> 返回
        </a-breadcrumb-item>
      </a-breadcrumb>
      <router-view v-if="!loading" />
    </page-container>-->
    <div class="container">
      <router-view v-if="!loading" />
    </div>
  </pro-layout>
</template>

<script setup>
import { getMenuData, clearMenuItem } from "@ant-design-vue/pro-layout";
import RightContent from "@/components/GlobalHeader/RightContent.vue";
import styleSetting, { defaultSetting } from "@/config/defaultSetting";

const locale = i18n => i18n;
const route = useRoute();
const router = useRouter();
const permission = usePermissionStore();
const [loading, setLoading] = useState(false);

const { menuData } = getMenuData(clearMenuItem(permission.$state.routes));

const title = computed(() => {
  return route.meta.title;
});

const state = ref({
  collapsed: false, // default value
  openKeys: [defaultSetting.defaultRoute],
  selectedKeys: [defaultSetting.defaultRoute]
});

const layoutConf = ref(styleSetting);

const handleBack = () => {
  router.back();
};

watch(
  route,
  cur => {
    state.value.openKeys = [/\/[a-zA-Z0-9]+/.exec(cur.fullPath)[0]];
    state.value.selectedKeys = [cur.path];
  },
  { immediate: true, deep: true }
);
</script>

<style lang="less" scoped>
@import "./BasicLayout.less";
</style>
