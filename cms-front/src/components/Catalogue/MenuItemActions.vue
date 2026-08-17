<!-- components/MenuItemActions.vue -->
<template>
  <!-- <span class="menu-item-actions" @click.stop>
    <UnorderedListOutlined v-if="showAdd" @click.stop="emit('add', item)" />
    <EditOutlined v-if="showEdit" @click.stop="emit('edit', item)" title="编辑" />
    <DeleteOutlined v-if="showDelete" @click.stop="emit('delete', item)" title="删除" />
    <SearchOutlined v-if="showView" @click.stop="emit('view', item)" title="查看" />
  </span> -->
  <a-dropdown class="menu-item-actions" @click.stop>
    <UnorderedListOutlined />
    <template #overlay>
      <a-menu>
        <a-menu-item v-if="item.type !== MENU_TYPE.ARTICLE">
          <a href="javascript:;" @click.stop="emit('add', item)">新增</a>
        </a-menu-item>
        <a-menu-item>
          <a href="javascript:;" @click.stop="emit('edit', item)">编辑</a>
        </a-menu-item>
        <a-menu-item>
          <a href="javascript:;" @click.stop="emit('delete', item)">删除</a>
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup>
import { UnorderedListOutlined } from '@ant-design/icons-vue';
import { MENU_TYPE } from '@/consts/enum';

const props = defineProps({
  // 当前菜单项数据
  item: {
    type: Object,
    required: true,
  },
  // 控制各按钮是否显示
  showAdd: {
    type: Boolean,
    default: true,
  },
  showEdit: {
    type: Boolean,
    default: true,
  },
  showDelete: {
    type: Boolean,
    default: true,
  },
  showView: {
    type: Boolean,
    default: true,
  },
});

const emit = defineEmits(['add', 'edit', 'delete', 'view']);
</script>

<style scoped>
.menu-item-actions {
  /* display: none; */
  display: inline-block;
   margin-right: 20px;
}
.menu-item-actions span[role="img"] {
  font-size: 14px;
  cursor: pointer;
  color: var(--highlight-color);
}
.menu-item-actions span[role="img"]:hover {
  color: var(--highlight-color);
}
</style>