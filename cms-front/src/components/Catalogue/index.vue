<template>
  <div class="catalogue">
    <div class="catalogue_content">
      <div class="catalogue_tool">
        <a-button
          size="small"
          @click="handleChangeMode"
          :type="mode === CATALOGUE_MODE.EDIT ? 'primary' : 'default'"
          v-auth
        >
          <template #icon>
            <RetweetOutlined />
          </template>
          {{ mode === CATALOGUE_MODE.EDIT ? "编辑" : "预览" }}
        </a-button>
        <a-button
          size="small"
          type="primary"
          @click="handleAddGroup"
          v-if="isEdit"
          style="margin-left: 10px;"
        >新增分组</a-button>
      </div>
      <a-menu
        v-model:openKeys="openKeys"
        v-model:selectedKeys="selectedKeys"
        mode="inline"
        @click="handleClick"
        class="catalogue_menu"
      >
        <div v-for="firstItem in list" :key="firstItem.id">
          <template v-if="firstItem.type === MENU_TYPE.GROUP">
            <a-menu-item-group :key="firstItem.id">
              <template #title>
                <div class="menu-item-content">
                  <span class="menu-item-label">{{ firstItem.name }}</span>
                  <MenuItemActions
                    v-if="isEdit"
                    :item="firstItem"
                    @add="handleAddItem(firstItem)"
                    @edit="handleEditGroup(firstItem)"
                    @delete="handleDeleteGroup(firstItem)"
                    @view="() => {}"
                  />
                </div>
              </template>

              <template v-if="firstItem?.items?.length">
                <div v-for="secondItem in firstItem.items" :key="secondItem.id">
                  <template v-if="secondItem.type === MENU_TYPE.MENU">
                    <a-sub-menu :key="secondItem.id">
                      <template #title>
                        <div class="menu-item-content">
                          <span class="menu-item-label">{{ secondItem.title }}</span>
                          <MenuItemActions
                            v-if="isEdit"
                            :item="secondItem"
                            @add="handleAddItem(firstItem,secondItem)"
                            @edit="handleEditItem(firstItem,secondItem)"
                            @delete="() => {}"
                            @view="() => {}"
                          />
                        </div>
                      </template>
                      <template v-if="secondItem?.children?.length">
                        <a-menu-item v-for="thirdItem in secondItem.children" :key="thirdItem.id">
                          <div class="menu-item-content">
                            <span class="menu-item-label">{{ thirdItem.title }}</span>
                            <MenuItemActions
                              v-if="isEdit"
                              :item="thirdItem"
                              @add="handleAddItem(thirdItem)"
                              @edit="handleEditItem(thirdItem)"
                              @delete="() => {}"
                              @view="() => {}"
                            />
                          </div>
                        </a-menu-item>
                      </template>
                    </a-sub-menu>
                  </template>

                  <template v-if="secondItem.type === MENU_TYPE.ARTICLE">
                    <a-menu-item :key="secondItem.id">
                      <div class="menu-item-content">
                        <span class="menu-item-label">{{ secondItem.title }}</span>
                        <MenuItemActions
                          v-if="isEdit"
                          :item="secondItem"
                          @add="handleAddItem(secondItem)"
                          @edit="handleEditItem(secondItem)"
                          @delete="() => {}"
                          @view="() => {}"
                        />
                      </div>
                    </a-menu-item>
                  </template>
                </div>
              </template>
            </a-menu-item-group>
          </template>

          <template v-if="firstItem.type === MENU_TYPE.MENU">
            <a-sub-menu :key="firstItem.id">
              <template #title>
                <div class="menu-item-content">
                  <span class="menu-item-label">{{ firstItem.label }}</span>
                  <MenuItemActions
                    v-if="isEdit"
                    :item="firstItem"
                    @add="handleAddItem(firstItem)"
                    @edit="handleEditItem(firstItem)"
                    @delete="() => {}"
                    @view="() => {}"
                  />
                </div>
              </template>

              <template v-if="firstItem?.children?.length">
                <a-menu-item v-for="secondItem in firstItem.children" :key="secondItem.id">
                  <div class="menu-item-content">
                    <span class="menu-item-label">{{ secondItem.label }}</span>
                    <MenuItemActions
                      v-if="isEdit"
                      :item="secondItem"
                      @add="handleAddItem(secondItem)"
                      @edit="handleEditItem(secondItem)"
                      @delete="() => {}"
                      @view="() => {}"
                    />
                  </div>
                </a-menu-item>
              </template>
            </a-sub-menu>
          </template>

          <template v-if="firstItem.type === MENU_TYPE.ARTICLE">
            <a-menu-item :key="firstItem.id">
              <div class="menu-item-content">
                <span class="menu-item-label">{{ firstItem.label }}</span>
                <MenuItemActions
                  v-if="isEdit"
                  :item="firstItem"
                  @add="handleAddItem(firstItem)"
                  @edit="handleEditItem(firstItem)"
                  @delete="() => {}"
                  @view="() => {}"
                />
              </div>
            </a-menu-item>
          </template>
        </div>
      </a-menu>
    </div>
    <GroupFormModal
      v-model:visible="groupFormModalVisible"
      :isEdit="isGroupEdit"
      :row="currentItem"
      @ok="handleGroupFormOk"
    />
    <ItemFormModal
      v-model:visible="addItemModalVisible"
      :isEdit="isAddItemEdit"
      :row="currentItem"
      @ok="handleItemFormOk"
    />
  </div>
</template>

<script setup>
import { MENU_TYPE, CATALOGUE_MODE } from "@/consts/enum";
import { findItemByAttr } from "@/utils/util";
import MenuItemActions from "./MenuItemActions.vue";
import GroupFormModal from "./GroupFormModal.vue";
import ItemFormModal from "./ItemFormModal.vue";
import { RetweetOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import { addGroup, editGroup, deleteGroup } from "@/service/group";
import { createItem, editItem, deleteItem } from "@/service/items";
import { getDirectoryTree } from "@/service/common";
import { useRouter } from "vue-router";
import { useGlobalStore } from "@/stores/global";

const global = useGlobalStore();
const router = useRouter();

const ACTIONS_PERMISSION = {
  [MENU_TYPE.GROUP]: {
    showAdd: true,
    showEdit: true,
    showDelete: true
  },
  [MENU_TYPE.MENU]: {
    showAdd: true,
    showEdit: true,
    showDelete: true
  },
  [MENU_TYPE.ARTICLE]: {
    showAdd: false,
    showEdit: true,
    showDelete: true
  }
};

const [openKeys, setOpenKeys] = useState([]);
const [selectedKeys, setSelectedKeys] = useState([]);
const [list, setList] = useState([]);
const [currentItem, setCurrentItem] = useState(null);
const [currentGroup, setCurrentGroup] = useState(null);
const [mode, setMode] = useState(CATALOGUE_MODE.PREVIEW);

const [groupFormModalVisible, setGroupFormModalVisible] = useState(false);
const [isGroupEdit, setIsGroupEdit] = useState(false);

const [addItemModalVisible, setAddItemModalVisible] = useState(false);
const [isAddItemEdit, setIsAddItemEdit] = useState(false);

const [source, setSource] = useState(null);

const isEdit = computed(() => mode.value === CATALOGUE_MODE.EDIT);

const emit = defineEmits(["articleClick"]);

const handleChangeMode = () => {
  setMode(
    mode.value === CATALOGUE_MODE.EDIT
      ? CATALOGUE_MODE.PREVIEW
      : CATALOGUE_MODE.EDIT
  );
};

const handleClick = e => {
  emit("articleClick", e);
};

const handleAddItem = (groupItem, item) => {
  setIsAddItemEdit(false);
  setCurrentGroup(groupItem);
  setCurrentItem(item);
  setAddItemModalVisible(true);
};

const handleEditItem = (groupItem, item) => {
  setIsAddItemEdit(true);
  setCurrentGroup(groupItem);
  setCurrentItem(item);
  setAddItemModalVisible(true);
};

const handleItemFormOk = async form => {
  try {
    const params = {
      ...form,
      source: source.value,
      groupId: currentGroup.value.id,
      parentId: currentItem.value.id
    }
    const res = isAddItemEdit.value ?  await editItem(params, currentItem.value.id) : await createItem(params)
    if (res.code === 0 || res.code === 200) {
      message.success(`${isAddItemEdit.value ? "新增" : "编辑"}条目成功`);
      getTree();
      setAddItemModalVisible(false);
    }
  } catch (error) {
    message.error(error.message);
  }
};

const handleAddGroup = () => {
  setCurrentGroup(null);
  setIsGroupEdit(false);
  setGroupFormModalVisible(true);
};

const handleEditGroup = item => {
  setCurrentGroup(item);
  setIsGroupEdit(true);
  setGroupFormModalVisible(true);
};

const handleDeleteGroup = item => {
  global.modal.confirm({
    title: "删除",
    content: `确定要删除分组“${item.name}”吗？`,
    async onOk() {
      try {
        const res = await deleteGroup(item.id);
        if (res.code === 0 || res.code === 200) {
          message.success("删除分组成功");
          getTree();
        }
      } catch (error) {
        message.error(error.message);
      }
    }
  });
};

const handleGroupFormOk = async form => {
  try {
    const params = {
      ...form,
      source: source.value
    }
    const res = isGroupEdit.value ? await editGroup(params, currentItem.value.id) : await addGroup(params);
    if (res.code === 0 || res.code === 200) {
      message.success(`${isGroupEdit.value ? "编辑" : "新增"}分组成功`);
      getTree(source);
      setGroupFormModalVisible(false);
    }
  } catch (error) {
    message.error(error.message);
  }
};

const getTree = async () => {
  try {
    const res = await getDirectoryTree(source.value);
    if (res.code === 0 || res.code === 200) {
      const data = (res.data || []).map(item => ({
        ...item,
        type: MENU_TYPE.GROUP
      }));
      setList(data);
    }
  } catch (error) {
    console.error("getTree", error);
  }
};

// 根据路由变化获取source
watch(
  router.currentRoute.value.path,
  () => {
    const s = router.currentRoute.value.path.split("/").pop();
    if (s) {
      setSource(s);
      getTree();
    }
  },
  { immediate: true }
)

onMounted(() => {
  const mock = [
    {
      label: "基础",
      id: 0,
      sort: 0,
      type: MENU_TYPE.GROUP,
      children: [
        {
          label: "营销获客营销获客营销获客营销获客营销获客营销获客",
          id: 10,
          sort: 0,
          type: MENU_TYPE.MENU,
          children: [
            {
              label: "这是测试文章测试文章测试文章这是测试文章测试",
              id: 20,
              sort: 0,
              type: MENU_TYPE.ARTICLE
            },
            {
              label: "这是测试文章测试文章测试文章",
              id: 200,
              sort: 1,
              type: MENU_TYPE.ARTICLE
            },
            {
              label: "这是测试文章测试文章测试文章",
              id: 2000,
              sort: 2,
              type: MENU_TYPE.ARTICLE
            },
            {
              label: "这是测试文章",
              id: 20000,
              sort: 3,
              type: MENU_TYPE.ARTICLE
            }
          ]
        },
        {
          label: "测试文章2测试文章2测试文章2测试文章2测试文章2",
          id: 30,
          sort: 1,
          type: MENU_TYPE.ARTICLE
        }
      ]
    },
    {
      label: "营销",
      id: 1,
      type: MENU_TYPE.GROUP,
      sort: 1,
      children: [
        {
          label: "营销获客",
          id: 11,
          sort: 0,
          type: MENU_TYPE.MENU,
          children: [
            {
              label: "这是测试文章测试文章测试文章",
              id: 21,
              sort: 0,
              type: MENU_TYPE.ARTICLE
            }
          ]
        },
        {
          label: "测试文章2",
          id: 31,
          sort: 1,
          type: MENU_TYPE.ARTICLE
        }
      ]
    },
    {
      label: "✅常见问题",
      id: 2,
      type: MENU_TYPE.GROUP,
      sort: 2,
      children: [
        {
          label: "营销获客",
          id: 111,
          sort: 0,
          type: MENU_TYPE.MENU,
          children: [
            {
              label: "这是测试文章测试文章测试文章",
              id: 222,
              sort: 0,
              type: MENU_TYPE.ARTICLE
            }
          ]
        },
        {
          label: "测试文章2",
          id: 333,
          sort: 1,
          type: MENU_TYPE.ARTICLE
        }
      ]
    },
    {
      label: "营销2",
      id: 3,
      type: MENU_TYPE.GROUP,
      sort: 3,
      children: [
        {
          label: "营销获客",
          id: 1111,
          sort: 0,
          type: MENU_TYPE.MENU,
          children: [
            {
              label: "这是测试文章测试文章测试文章",
              id: 2222,
              sort: 0,
              type: MENU_TYPE.ARTICLE
            }
          ]
        },
        {
          label: "测试文章2",
          id: 3333,
          sort: 1,
          type: MENU_TYPE.ARTICLE
        }
      ]
    },
    {
      label: "营销3",
      id: 4,
      type: MENU_TYPE.GROUP,
      sort: 4,
      children: [
        {
          label: "营销获客",
          id: 11111,
          sort: 0,
          type: MENU_TYPE.MENU,
          children: [
            {
              label: "这是测试文章测试文章测试文章",
              id: 22222,
              sort: 0,
              type: MENU_TYPE.ARTICLE
            }
          ]
        },
        {
          label: "测试文章2",
          id: 33333,
          sort: 1,
          type: MENU_TYPE.ARTICLE
        }
      ]
    },
    {
      label: "测试文章2222222测试文章2222222测试文章2222222",
      id: 999999,
      type: MENU_TYPE.ARTICLE,
      sort: 5
    }
  ];

  // 根据当前路径获取source
  /* const source = window.location.pathname.split("/").pop();
  setSource(source);
  getTree(); */
});
</script>

<style scoped lang="less">
.catalogue {
  display: flex;
  flex-direction: row-reverse;
  height: 100%;
  padding-top: 24px;
  &_menu {
    width: 240px;
    height: 100%;
    overflow-y: auto;
  }
  &_tool {
    margin: 0 0 6px 24px;
  }

  /* :deep {
    .ant-menu-item-group-title:hover,
    .ant-menu-submenu-title:hover,
    .ant-menu-item:hover {
      .menu-item-actions {
        display: inline-block !important;
      }
    }

    .ant-menu-submenu-title:hover {
      .menu-item-actions {
        margin-right: 20px;
      }
    }
  } */

  .menu-item-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
  }
}
</style>