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
                  <span class="menu-item-label">{{ firstItem.title }}</span>
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
                              @edit="handleEditItem(thirdItem, secondItem)"
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
                          @edit="handleEditItem(secondItem, firstItem)"
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
                  <span class="menu-item-label">{{ firstItem.title }}</span>
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
                    <span class="menu-item-label">{{ secondItem.title }}</span>
                    <MenuItemActions
                      v-if="isEdit"
                      :item="secondItem"
                      @add="handleAddItem(secondItem)"
                      @edit="handleEditItem(secondItem, firstItem)"
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
                <span class="menu-item-label">{{ firstItem.title }}</span>
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
      :row="currentGroup"
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
import { addGroup, editGroup, deleteGroup, getGroupList } from "@/service/group";
import { createItem, editItem } from "@/service/items";
import { useRouter } from "vue-router";
import { useGlobalStore } from "@/stores/global";

const global = useGlobalStore();
const router = useRouter();

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
    type: node.type
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

const handleDeleteGroup = item => {
  global.modal.confirm({
    title: "删除",
    content: `确定要删除分组“${item.title}”吗？`,
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
    const res = await getGroupList(spaceSlug);
    if (res.code === 0 || res.code === 200) {
      setList(res.data || []);
    }
  } catch (error) {
    console.error("getTree", error);
  }
};

watch(
  () => router.currentRoute.value.path,
  () => {
    const s = router.currentRoute.value.path.split("/").pop();
    if (s) {
      setSource(s);
      getTree(s);
    }
  },
  { immediate: true }
);
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

  .menu-item-content {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
  }
}
</style>
