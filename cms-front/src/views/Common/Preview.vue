<template>
  <div class="customer">
    <ProTable
      :request="handleQuery"
      :columns="columns"
      :scroll="{ x: 1300 }"
      ref="actionRef"
      rowKey="id"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'title'">
          <longer-text :lineCount="1" :content="record.title" />
        </template>
        <template v-if="column.key === 'type'">
          <a-tag>{{ MENU_TYPE_MSG[record.type] }}</a-tag>
        </template>
        <template v-if="column.key === 'status'">
          <span :style="{ color: NODE_STATUS_MSG[record.status].color }">{{
            NODE_STATUS_MSG[record.status].text
          }}</span>
        </template>
        <template v-if="column.key === 'action'">
          <div>
            <throttle-button type="link" @click="handleAction(record)" class="no-padding-btn"
              >编辑</throttle-button
            >
            <throttle-button type="link" @click="handleDelete(record)"
              >删除</throttle-button
            >
            <throttle-button type="link" @click="handlePreview(record)" class="no-padding-btn"
              >预览</throttle-button
            >
          </div>
        </template>
      </template>
    </ProTable>
  </div>
</template>

<script setup>
import ProTable from "@/components/ProTable/index.vue";
import dayjs from "dayjs";
import { formLayoutInModal, formLayoutInModalSpan6 } from "@/consts/const";
import {
  MENU_TYPE,
  MENU_TYPE_MSG,
  NODE_STATUS,
  NODE_STATUS_MSG,
} from "@/consts/enum";
import { copyImgageToClipboard, downloadImage } from "@/utils/util";
import { ref } from "vue";
import { useRouter } from 'vue-router'

const router = useRouter()

const columns = [
  {
    title: "标题",
    key: "title",
    scopedSlots: { customRender: "title" },
  },
  {
    title: "类型",
    key: "type",
    scopedSlots: { customRender: "type" },
    hideInSearch: true,
  },
  {
    title: "状态",
    dataIndex: "status",
    key: "status",
    scopedSlots: { customRender: "status" },
    valueType: "SELECT",
    defaultValue: null,
    custom: {
      enum: { ...NODE_STATUS_MSG },
      showAll: true,
    },
  },
  {
    title: "更新时间",
    key: "updateTime",
    dataIndex: "updateTime",
    hideInSearch: true,
  },
  {
    title: "操作",
    dataIndex: "action",
    key: "action",
    fixed: "right",
    width: 180,
    hideInSearch: true,
  },
];

const formRef = ref();
const [form, setForm] = useState({});
const global = useGlobalStore();

const handlePreview = (row) => {
  router.push({ path: '/preview', params: row.id })
}

const handleAction = (row) => {}

const handleDelete = (row) => {
  global.modal.confirm({
    title: "删除",
    content: `确定要删除${MENU_TYPE_MSG[row.type]}“${row.title}”吗？`,
    async onOk() {},
  });
};

// 查询列表
const handleQuery = async (params) => {
  const mock = [
    {
      id: 0,
      title: "常见问题",
      type: MENU_TYPE.ARTICLE,
      status: NODE_STATUS.ONLINE,
      updateTime: "2025-09-05 16:37:00",
    },
    {
      id: 1,
      title: "产品更新",
      type: MENU_TYPE.MENU,
      status: NODE_STATUS.ONLINE,
      updateTime: "2025-09-05 16:37:00",
      children: [
        {
          id: 3,
          title: "更新日志",
          type: MENU_TYPE.MENU,
          status: NODE_STATUS.ONLINE,
          updateTime: "2025-09-05 16:37:00",
          children: [
            {
              id: 4,
              title: "2025.08.24",
              type: MENU_TYPE.ARTICLE,
              status: NODE_STATUS.ONLINE,
              updateTime: "2025-09-05 16:37:00",
            },
          ],
        },
      ],
    },
    {
      id: 2,
      title: "基础",
      type: MENU_TYPE.MENU,
      status: NODE_STATUS.ONLINE,
      updateTime: "2025-09-05 16:37:00",
      children: [
        {
          id: 5,
          title: "获客营销",
          type: MENU_TYPE.MENU,
          status: NODE_STATUS.ONLINE,
          updateTime: "2025-09-05 16:37:00",
          children: [
            {
              id: 6,
              title: "渠道活码",
              type: MENU_TYPE.ARTICLE,
              status: NODE_STATUS.ONLINE,
              updateTime: "2025-09-05 16:37:00",
            },
          ],
        },
        {
          id: 7,
          title: "获客营销",
          type: MENU_TYPE.ARTICLE,
          status: NODE_STATUS.ONLINE,
          updateTime: "2025-09-05 16:37:00",
        },
      ],
    },
  ];

  return Promise.resolve({
    data: mock,
    total: 3,
  });
};
</script>

<style lang="less" scoped>
.row {
  height: 24px;
  margin-bottom: 4px;
  white-space: nowrap;
}

.base-tag {
  color: orange;
  display: inline-block;
  margin-left: 6px;
}

.error-tag {
  color: rgba(244, 102, 115, 1);
  display: inline-block;
  margin-left: 6px;
}

.code-box {
  .img-box {
    width: 200px;
    height: 200px;
    margin: 36px 0;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  img {
    width: 100%;
    height: 100%;
    margin: 36px 0;
  }

  .button {
    button {
      width: 100px;
      margin: 0 12px;
    }
  }
}
</style>
