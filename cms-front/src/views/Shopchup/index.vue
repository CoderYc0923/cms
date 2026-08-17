<template>
  <div class="flex-box-row" style="height: 100%">
    <div class="flex-25">
      <Catalogue @articleClick="handleArticleClick" />
    </div>
    <div class="flex-75">
      <!--  <Article /> -->
      <Preview :content="content" :title="title" v-show="content" />
      <Empty v-show="!content" />
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

const INIT_TITLE = "请输入文章标题...";
const INIT_CONTENT = "请输入文章内容...";

const formRef = ref();
const [form, setForm] = useState({});
const [activeMenu, setActiveMenu] = useState(null);
const [content, setContent] = useState("");
const [title, setTitle] = useState("");

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

const handleArticleClick = (id) => {
  console.log("handleArticleClick", id);
  handleGetArticle(id);
};

const openArticle = (id, type) => {
  setCurrentId(id);
  setCurrentActionType(type);
};

const handleGetArticle = (id) => {
  console.log("handleGetArticle", id);

  setTitle(INIT_TITLE);
  setContent(INIT_CONTENT);

 /*  setTitle(`从接口获取文章标题${id}`);
  setContent(`
    <h2>一、功能介绍</h2>
    <p>渠道活码能够<strong>创建带参数的二维码</strong>，把不同渠道扫码过来的<strong>客户分流</strong>给不同的员工接待，并自动给客户打上渠道标签，记录各个渠道添加的客户数量，达到客户分层、统计不同渠道推广效果的目的。</p>
    <h3>1.1 功能介绍1</h3>
    <p>渠道活码能够<strong>创建带参数的二维码</strong>，把不同渠道扫码过来的<strong>客户分流</strong>给不同的员工接待，并自动给客户打上渠道标签，记录各个渠道添加的客户数量，达到客户分层、统计不同渠道推广效果的目的。</p>
    <h3>1.2 功能介绍2</h3>
    <p>渠道活码能够<strong>创建带参数的二维码</strong>，把不同渠道扫码过来的<strong>客户分流</strong>给不同的员工接待，并自动给客户打上渠道标签，记录各个渠道添加的客户数量，达到客户分层、统计不同渠道推广效果的目的。</p>
    <h3>1.3 功能介绍3</h3>
    <h4>1.3.1 功能介绍3.1</h4>
    <p>渠道活码能够<strong>创建带参数的二维码</strong>，把不同渠道扫码过来的<strong>客户分流</strong>给不同的员工接待，并自动给客户打上渠道标签，记录各个渠道添加的客户数量，达到客户分层、统计不同渠道推广效果的目的。</p>
    <h4>1.3.2 功能介绍3.2</h4>
    <p>渠道活码能够<strong>创建带参数的二维码</strong>，把不同渠道扫码过来的<strong>客户分流</strong>给不同的员工接待，并自动给客户打上渠道标签，记录各个渠道添加的客户数量，达到客户分层、统计不同渠道推广效果的目的。</p>
    <h4>1.3.3 功能介绍3.3</h4>
    <p>渠道活码能够<strong>创建带参数的二维码</strong>，把不同渠道扫码过来的<strong>客户分流</strong>给不同的员工接待，并自动给客户打上渠道标签，记录各个渠道添加的客户数量，达到客户分层、统计不同渠道推广效果的目的。</p>
    <h3>1.4 功能介绍4</h3>
    <p>渠道活码能够<strong>创建带参数的二维码</strong>，把不同渠道扫码过来的<strong>客户分流</strong>给不同的员工接待，并自动给客户打上渠道标签，记录各个渠道添加的客户数量，达到客户分层、统计不同渠道推广效果的目的。</p>
    <h2>二、使用场景</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>三、使用权限</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>四、使用权限1</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>五、使用权限2</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>六、使用权限3</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>七、使用权限4</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>八、使用权限5</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>九、使用权限6</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>十、使用权限6</h2>  
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>十一、使用权限6</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>十二、使用权限6</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>十三、使用权限6</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>十四、使用权限6</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>十五、使用权限6</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
    <h2>十六、使用权限6</h2>
    <p>适用于企业线上、线下不同渠道的引流推广。只需将渠道活码下载下来，放在小程序、公众号或线下各引流场景中使用，记录各个渠道添加的客户数量，统计不同渠道推广效果，分析流量来源，帮助企业寻找优质投放渠道，全程跟进记录营销活动的每一个重要转化节点。</p>
  `); */
};

onMounted(() => {
});
</script>

<style lang="less" scoped>
</style>
