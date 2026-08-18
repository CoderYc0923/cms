<template>
  <div class="flex-box-row" style="height: 100%">
    <div class="flex-25">
      <Catalogue @articleClick="handleArticleClick" />
    </div>
    <div class="flex-75">
      <!--  <Article /> -->
      <Preview :content="content" :title="title" v-show="hasArticle" />
      <Empty v-show="!hasArticle" />
    </div>
  </div>
</template>

<script setup>
import ProTable from "@/components/ProTable/index.vue";
import Catalogue from "@/components/Catalogue/index.vue";
import Article from "@/components/Article/index.vue";
import Preview from "@/components/Preview/index.vue";
import Empty from "@/components/Empty/index.vue";

import dayjs from "dayjs";
import { formLayoutInModal, formLayoutInModalSpan6 } from "@/consts/const";
import {
  MENU_TYPE,
  MENU_TYPE_MSG,
  NODE_STATUS,
  NODE_STATUS_MSG,
  ACTION_TYPE
} from "@/consts/enum";
import { copyImgageToClipboard, downloadImage } from "@/utils/util";
import { getArticle } from "@/service/items";
import { ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

const columns = [
  {
    title: "标题",
    key: "title",
    scopedSlots: { customRender: "title" }
  },
  {
    title: "类型",
    key: "type",
    scopedSlots: { customRender: "type" },
    hideInSearch: true
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
      showAll: true
    }
  },
  {
    title: "更新时间",
    key: "updateTime",
    dataIndex: "updateTime",
    hideInSearch: true
  },
  {
    title: "操作",
    dataIndex: "action",
    key: "action",
    fixed: "right",
    width: 180,
    hideInSearch: true
  }
];

const formRef = ref();
const [form, setForm] = useState({});
const [activeMenu, setActiveMenu] = useState(null);
const [content, setContent] = useState("");
const [title, setTitle] = useState("");
const [hasArticle, setHasArticle] = useState(false);

const global = useGlobalStore();

const handleDelete = row => {
  global.modal.confirm({
    title: "删除",
    content: `确定要删除${MENU_TYPE_MSG[row.type]}“${row.title}”吗？`,
    async onOk() {}
  });
};

const handleMenuChange = item => {
  setActiveMenu(item);
};

const handleArticleClick = node => {
  handleGetArticle(node);
};

const openArticle = (id, type) => {
  setCurrentId(id);
  setCurrentActionType(type);
};

const handleGetArticle = async node => {
  if (!node?.nodeId) {
    return;
  }
  try {
    const res = await getArticle(node.nodeId);
    if (res.code === 0 || res.code === 200) {
      const data = res.data || {};
      setTitle(node.title || "");
      setContent(data.content || "");
      setHasArticle(true);
    }
  } catch (error) {
    setHasArticle(false);
    setTitle("");
    setContent("");
  }
};

onMounted(() => {
});
</script>

<style lang="less" scoped>
</style>
