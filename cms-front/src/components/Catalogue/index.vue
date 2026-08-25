<template>
  <div class="catalogue">
    <div class="catalogue_content">
      <div class="catalogue_tool" v-if="!readonly">
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
                  <span class="menu-item-label">{{ firstItem.title }}</span>
                  <MenuItemActions
                    v-if="isEdit"
                    :item="firstItem"
                    @add="handleAddItem(firstItem)"
                    @edit="handleEditGroup(firstItem)"
                    @delete="handleDeleteNode"
                    @view="() => {}"
                  />
                </div>
              </template>

              <template v-if="firstItem?.children?.length">
                <div v-for="secondItem in firstItem.children" :key="secondItem.id">
                  <template v-if="secondItem.type === MENU_TYPE.MENU">
                    <a-sub-menu :key="secondItem.id">
                      <template #title>
                        <div class="menu-item-content">
                          <span class="menu-item-label">{{ secondItem.title }}</span>
                          <MenuItemActions
                            v-if="isEdit"
                            :item="secondItem"
                            @add="handleAddItem(secondItem)"
                            @edit="handleEditItem(secondItem, firstItem)"
                            @delete="handleDeleteNode"
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
                              @edit="handleEditItem(thirdItem, secondItem)"
                              @delete="handleDeleteNode"
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
                          @edit="handleEditItem(secondItem, firstItem)"
                          @delete="handleDeleteNode"
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
                  <span class="menu-item-label">{{ firstItem.title }}</span>
                  <MenuItemActions
                    v-if="isEdit"
                    :item="firstItem"
                    @add="handleAddItem(firstItem)"
                    @edit="handleEditItem(firstItem)"
                    @delete="handleDeleteNode"
                    @view="() => {}"
                  />
                </div>
              </template>

              <template v-if="firstItem?.children?.length">
                <a-menu-item v-for="secondItem in firstItem.children" :key="secondItem.id">
                  <div class="menu-item-content">
                    <span class="menu-item-label">{{ secondItem.title }}</span>
                    <MenuItemActions
                      v-if="isEdit"
                      :item="secondItem"
                      @add="handleAddItem(secondItem)"
                      @edit="handleEditItem(secondItem, firstItem)"
                      @delete="handleDeleteNode"
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
                <span class="menu-item-label">{{ firstItem.title }}</span>
                <MenuItemActions
                  v-if="isEdit"
                  :item="firstItem"
                  @add="handleAddItem(firstItem)"
                  @edit="handleEditItem(firstItem)"
                      @delete="handleDeleteNode"
                  @view="() => {}"
                />
              </div>
            </a-menu-item>
          </template>
        </div>
      </a-menu>
    </div>
    <GroupFormModal
      v-if="!readonly"
      v-model:visible="groupFormModalVisible"
      :isEdit="isGroupEdit"
      :row="currentGroup"
      @ok="handleGroupFormOk"
    />
    <ItemFormModal
      v-if="!readonly"
      v-model:visible="addItemModalVisible"
      :isEdit="isAddItemEdit"
      :row="currentItem"
      @ok="handleItemFormOk"
    />
  </div>
</template>

<script setup>
import { MENU_TYPE, MENU_TYPE_MSG, CATALOGUE_MODE } from "@/consts/enum";
import { findItemByAttr } from "@/utils/util";
import MenuItemActions from "./MenuItemActions.vue";
import GroupFormModal from "./GroupFormModal.vue";
import ItemFormModal from "./ItemFormModal.vue";
import { RetweetOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import { addGroup, editGroup, deleteGroup, getGroupList } from "@/service/group";
import { createItem, editItem } from "@/service/items";
import { getPublicTree } from "@/shared/api/public";
import { useRoute } from "vue-router";
import { useGlobalStore } from "@/stores/global";
import { useUserStore } from "@/stores/user";

const props = defineProps({
  readonly: {
    type: Boolean,
    default: false
  },
  spaceSlug: {
    type: String,
    default: ""
  }
});

const global = useGlobalStore();
const route = useRoute();
const userStore = useUserStore();

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

const isEdit = computed(() => !props.readonly && mode.value === CATALOGUE_MODE.EDIT);

const emit = defineEmits(["articleClick", "nodeDeleted"]);

const buildNodePayload = (form, extra = {}) => {
  const params = {
    slug: source.value,
    title: form.title,
    type: form.type,
    sort: form.sort,
    ...extra
  };
  if (params.parentId == null) {
    delete params.parentId;
  }
  return params;
};

const handleChangeMode = () => {
  setMode(
    mode.value === CATALOGUE_MODE.EDIT
      ? CATALOGUE_MODE.PREVIEW
      : CATALOGUE_MODE.EDIT
  );
};

const handleClick = e => {
  const nodeId = Number(e.key);
  if (!nodeId) {
    return;
  }
  const node = findItemByAttr(list.value, "id", nodeId);
  if (node?.type !== MENU_TYPE.ARTICLE) {
    return;
  }
  emit("articleClick", {
    nodeId: node.id,
    title: node.title,
    type: node.type,
    sort: node.sort
  });
};

const handleAddItem = parent => {
  setIsAddItemEdit(false);
  setCurrentGroup(parent);
  setCurrentItem(null);
  setAddItemModalVisible(true);
};

const handleEditItem = (item, parent) => {
  setIsAddItemEdit(true);
  setCurrentGroup(parent || null);
  setCurrentItem(item);
  setAddItemModalVisible(true);
};

const handleItemFormOk = async form => {
  try {
    const params = buildNodePayload(form, {
      parentId: currentGroup.value?.id
    });
    const res = isAddItemEdit.value
      ? await editItem(params, currentItem.value.id)
      : await createItem(params);
    if (res.code === 0 || res.code === 200) {
      message.success(`${isAddItemEdit.value ? "编辑" : "新增"}条目成功`);
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

const hasChildren = item =>
  Array.isArray(item?.children) && item.children.length > 0;

const handleDeleteNode = item => {
  const typeLabel = MENU_TYPE_MSG[item.type] || "节点";

  if (
    (item.type === MENU_TYPE.GROUP || item.type === MENU_TYPE.MENU) &&
    hasChildren(item)
  ) {
    message.warning(`${typeLabel}下存在子节点，请先删除子节点`);
    return;
  }

  global.modal.confirm({
    title: "删除",
    content: `确定要删除${typeLabel}「${item.title}」吗？此操作不可恢复。`,
    okText: "删除",
    okType: "danger",
    cancelText: "取消",
    async onOk() {
      try {
        const res = await deleteGroup(item.id);
        if (res.code === 0 || res.code === 200) {
          message.success(`删除${typeLabel}成功`);
          if (selectedKeys.value.includes(String(item.id))) {
            setSelectedKeys([]);
          }
          emit("nodeDeleted", { nodeId: item.id, type: item.type });
          getTree();
        }
      } catch (error) {
        message.error(error.message || "删除失败");
      }
    }
  });
};

const handleGroupFormOk = async form => {
  try {
    const params = buildNodePayload({
      ...form,
      type: MENU_TYPE.GROUP
    });
    const res = isGroupEdit.value
      ? await editGroup(params, currentGroup.value.id)
      : await addGroup(params);
    if (res.code === 0 || res.code === 200) {
      message.success(`${isGroupEdit.value ? "编辑" : "新增"}分组成功`);
      getTree();
      setGroupFormModalVisible(false);
    }
  } catch (error) {
    message.error(error.message);
  }
};

const getTree = async slug => {
  const spaceSlug = slug || source.value;
  if (!spaceSlug) {
    return;
  }
  try {
    const fetchTree = props.readonly ? getPublicTree : getGroupList;
    const res = await fetchTree(spaceSlug);
    if (res.code === 0 || res.code === 200) {
      setList(res.data || []);
    }
  } catch (error) {
    console.error("getTree", error);
  }
};

const resolveSpaceSlug = () => {
  if (props.spaceSlug) {
    return props.spaceSlug;
  }
  const path = route.path.replace(/\/+$/, "");
  return path.split("/").filter(Boolean).pop() || "";
};

const loadTree = (slug) => {
  const spaceSlug = slug || resolveSpaceSlug();
  if (!spaceSlug) {
    return;
  }
  if (!props.readonly && !userStore.isLoggedIn) {
    return;
  }
  setSource(spaceSlug);
  getTree(spaceSlug);
};

watch(
  () => [resolveSpaceSlug(), userStore.isLoggedIn, props.readonly],
  () => {
    loadTree();
  },
  { immediate: true }
);
</script>

<style scoped lang="less">
.catalogue {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 16px 0 16px 16px;
  box-sizing: border-box;

  &_content {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
  }

  &_tool {
    flex-shrink: 0;
    margin: 0 16px 12px 0;
  }

  &_menu {
    flex: 1;
    width: 100%;
    min-height: 0;
    overflow-y: auto;
    border-inline-end: none !important;
  }

  .menu-item-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    gap: 8px;
  }
}
</style>
