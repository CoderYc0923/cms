<template>
  <div class="catalogue">
    <div class="catalogue_content">
      <div v-if="!readonly" class="catalogue_header" v-auth>
        <span class="catalogue_header-title">页面</span>
        <button
          v-if="isEdit"
          type="button"
          class="catalogue_header-action"
          title="新增分组"
          @click="handleAddGroup"
        >
          <PlusOutlined />
        </button>
      </div>
      <a-menu
        v-model:openKeys="openKeys"
        v-model:selectedKeys="selectedKeys"
        mode="inline"
        @click="handleClick"
        class="catalogue_menu"
      >
        <div v-for="(firstItem, index) in list" :key="firstItem.id" class="catalogue_section" :class="{ 'catalogue_section--spaced': index > 0 }">
          <template v-if="firstItem.type === MENU_TYPE.GROUP">
            <a-menu-item-group :key="firstItem.id">
              <template #title>
                <div class="menu-group-title">
                  <span class="menu-group-title__mark" aria-hidden="true" />
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
      <div v-if="!readonly" class="catalogue_footer" v-auth>
        <div class="catalogue_mode-switch">
          <button
            type="button"
            class="catalogue_mode-switch__item"
            :class="{ 'is-active': mode === CATALOGUE_MODE.PREVIEW }"
            @click="setCatalogueMode(CATALOGUE_MODE.PREVIEW)"
          >
            浏览
          </button>
          <button
            type="button"
            class="catalogue_mode-switch__item"
            :class="{ 'is-active': mode === CATALOGUE_MODE.EDIT }"
            @click="setCatalogueMode(CATALOGUE_MODE.EDIT)"
          >
            编辑
          </button>
        </div>
      </div>
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
import { PlusOutlined } from "@ant-design/icons-vue";
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
  },
  initialNodeId: {
    type: Number,
    default: undefined
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

let treeRequestSeq = 0;

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

const setCatalogueMode = nextMode => {
  setMode(nextMode);
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

  const requestSeq = ++treeRequestSeq;

  try {
    const fetchTree = props.readonly ? getPublicTree : getGroupList;
    const res = await fetchTree(spaceSlug);
    if (requestSeq !== treeRequestSeq || spaceSlug !== source.value) {
      return;
    }
    if (res.code === 0 || res.code === 200) {
      setList(res.data || []);
    }
  } catch (error) {
    if (requestSeq !== treeRequestSeq) {
      return;
    }
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
  if (source.value !== spaceSlug) {
    setList([]);
    setSelectedKeys([]);
    setOpenKeys([]);
  }
  setSource(spaceSlug);
  getTree(spaceSlug);
};

const findArticleNode = (items, nodeId) => {
  for (const item of items || []) {
    if (item.id === nodeId && item.type === MENU_TYPE.ARTICLE) {
      return item;
    }
    const found = findArticleNode(item.children, nodeId);
    if (found) {
      return found;
    }
  }
  return null;
};

const selectNode = nodeId => {
  if (!nodeId) {
    return;
  }
  const node = findArticleNode(list.value, nodeId);
  if (!node) {
    return;
  }
  setSelectedKeys([String(node.id)]);
  emit("articleClick", {
    nodeId: node.id,
    title: node.title,
    type: node.type,
    sort: node.sort
  });
};

watch(
  () => [resolveSpaceSlug(), userStore.isLoggedIn, props.readonly],
  () => {
    loadTree();
  },
  { immediate: true }
);

watch(
  () => [list.value, props.initialNodeId],
  () => {
    if (props.initialNodeId && list.value?.length) {
      selectNode(props.initialNodeId);
    }
  }
);

defineExpose({
  refresh: () => getTree(resolveSpaceSlug()),
  selectNode
});
</script>

<style scoped lang="less">
.catalogue {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 8px 6px 12px 8px;
  box-sizing: border-box;

  &_content {
    display: flex;
    flex-direction: column;
    height: 100%;
    min-height: 0;
  }

  &_header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex-shrink: 0;
    margin: 0 6px 4px 0;
    padding: 4px 8px 8px;

    &-title {
      font-size: 12px;
      font-weight: 500;
      color: var(--color-text-tertiary);
    }

    &-action {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 24px;
      height: 24px;
      padding: 0;
      border: none;
      border-radius: var(--radius-sm);
      background: transparent;
      color: var(--color-text-secondary);
      cursor: pointer;
      transition: background 0.15s ease, color 0.15s ease;

      &:hover {
        background: var(--color-bg-hover);
        color: var(--color-text-primary);
      }
    }
  }

  &_footer {
    flex-shrink: 0;
    margin: 16px 6px 0 0;
    padding-top: 16px;
  }

  &_mode-switch {
    display: flex;
    padding: 3px;
    border-radius: var(--radius-sm);
    background: var(--color-border-subtle);

    &__item {
      flex: 1;
      padding: 5px 0;
      border: none;
      border-radius: 4px;
      background: transparent;
      font-size: var(--text-caption);
      color: var(--color-text-secondary);
      cursor: pointer;
      transition: background 0.15s ease, color 0.15s ease, box-shadow 0.15s ease;

      &:hover:not(.is-active) {
        color: var(--color-text-primary);
      }

      &.is-active {
        background: var(--color-bg-surface);
        color: var(--color-text-primary);
        font-weight: 500;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
      }
    }
  }

  &_section {
    &--spaced {
      margin-top: 20px;
    }
  }

  &_menu {
    flex: 1;
    width: 100%;
    min-height: 0;
    overflow-y: auto;
    border-inline-end: none !important;
    background: transparent !important;
  }

  .menu-item-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    gap: 8px;
  }

  .menu-item-label {
    font-size: var(--text-label);
    line-height: 20px;
    color: var(--color-text-primary);
  }

  .menu-group-title {
    display: flex;
    align-items: center;
    gap: 8px;
    width: 100%;
    min-width: 0;

    &__mark {
      flex-shrink: 0;
      width: 2px;
      height: 12px;
      border-radius: 1px;
      background: var(--color-primary);
      opacity: 0.35;
    }

    .menu-item-label {
      font-size: 13px;
      font-weight: 500;
      color: var(--color-text-tertiary);
    }
  }

  :deep(.ant-menu) {
    font-size: var(--text-label);
    color: var(--color-text-primary);
    background: transparent;
  }

  :deep(.ant-menu-item),
  :deep(.ant-menu-submenu-title) {
    height: 32px !important;
    line-height: 32px !important;
    margin-block: 1px;
    padding-inline: 8px !important;
    border-radius: var(--radius-sm);
  }

  :deep(.ant-menu-item-selected) {
    background: var(--color-bg-hover) !important;
    color: var(--color-text-primary) !important;

    .menu-item-label {
      color: var(--color-text-primary);
      font-weight: 500;
    }
  }

  :deep(.ant-menu-item:hover),
  :deep(.ant-menu-submenu-title:hover) {
    background: var(--color-bg-hover) !important;
  }

  :deep(.ant-menu-item-group) {
    &::after {
      display: none !important;
    }

    &:not(:first-child) {
      margin-top: 20px;
    }
  }

  :deep(.ant-menu-item-group-list) {
    margin: 0;
    padding: 0;
  }

  :deep(.ant-menu-item-group-title) {
    height: auto !important;
    min-height: 0 !important;
    margin: 0 0 2px !important;
    padding: 0 8px 4px !important;
    line-height: 16px !important;

    &::before {
      display: none !important;
    }
  }

  :deep(.ant-menu-submenu .ant-menu-sub) {
    padding-inline-start: 4px !important;
  }

  :deep(.ant-menu-inline .ant-menu-sub.ant-menu-inline) {
    background: transparent !important;
  }
}
</style>
