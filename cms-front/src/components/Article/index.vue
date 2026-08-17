<template>
  <div class="action">
    <div class="content">
      <rich-text ref="richTextRef" :id="props.id" />
    </div>
    <!-- <div class="tools">
      <a-button type="primary" @click="onSubmit">
        {{
        isEdit ? "保存" : "创建"
        }}
      </a-button>
      <a-button type="primary" danger style="margin-left: 10px" @click="onPreview">内容预览</a-button>
      <a-button style="margin-left: 10px">取消</a-button>
    </div>

    <a-modal
      v-model:open="previewModalOpen"
      title="预览"
      :footer="null"
      width="50%"
      :bodyStyle="{ padding: '0' }"
      centered
    >
      <div class="preview-box preview-style" v-html="richTextHtml"></div>
    </a-modal> -->
  </div>
</template>

<script setup>
import dayjs from "dayjs";
import { ref } from "vue";
import { useRouter } from "vue-router";
import { MENU_TYPE, MENU_TYPE_MSG } from "@/consts/enum";

const props = defineProps({
  content: {
    type: String,
    default: ""
  }
});

const router = useRouter();
const route = useRoute();
const global = useGlobalStore();
const richTextRef = ref();
const [isEdit, setIsEdit] = useState(false);
const [previewModalOpen, setPreviewModalOpen] = useState(false);
const [richTextHtml, setRichTextHtml] = useState("");

const queryArticle = id => {};

const onSubmit = () => {};

const onPreview = () => {
  const html = richTextRef.value?.getRichTextHtml();
  console.log("html", html);

  setRichTextHtml(html || "");
  setPreviewModalOpen(true);
};

watch(
  () => props.id,
  id => {
    id && queryArticle(id);
  },
  {
    immediate: true
  }
);
</script>

<style lang="less" scoped>
.action {
  height: 100%;
  background: #fff;
  padding: 24px;
  display: flex;
  flex-direction: column;
}
.content {
 flex: 1;
}
.tools {
  margin-top: 12px;
  height: 48px;
}
.preview-box {
  width: 100%;
  background: #fff;
  height: 700px;
  padding: 24px;
  overflow: hidden;
  overflow-y: auto;
}
</style>
