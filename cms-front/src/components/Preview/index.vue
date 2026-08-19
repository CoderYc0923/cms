<template>
  <div class="preview">
    <div ref="contentRef" class="preview-content hide-scrollbar">
      <h1 class="preview-content-title" v-if="status !== ACTION_STATUS.EDIT">{{ title }}</h1>
      <a-textarea
        v-model:value="title"
        auto-size
        class="preview-content-title preview-content-title-editor"
        placeholder="请输入文章标题..."
        v-else
      />
      <div class="preview-style" v-html="richTextHtml" v-show="status !== ACTION_STATUS.EDIT"></div>
      <RichText
        v-show="status === ACTION_STATUS.EDIT"
        :content="richTextHtml"
        ref="richTextRef"
        :id="123"
      />
    </div>
    <div class="preview-sidebar">
      <div class="preview-sidebar-tools" v-if="!readonly">
        <template v-if="status === ACTION_STATUS.SAVE">
          <a-button type="primary" @click="handleEdit">编辑</a-button>
        </template>
        <template v-else>
          <a-button
            type="primary"
            @click="handlePreview"
          >{{ status === ACTION_STATUS.PREVIEW ? '取消预览' : '预览' }}</a-button>
          <a-button type="primary" class="no-first-btn" @click="handleSave">保存</a-button>
          <a-button class="no-first-btn" @click="handleCancel">取消</a-button>
        </template>
      </div>
      <div class="preview-sidebar-toc hide-scrollbar">
        <Anchor :anchorItems="anchorItems" :container="() => contentRef" />
      </div>
    </div>
  </div>
</template>

<script setup>
import Anchor from "@/components/Anchor/index.vue";
import RichText from "@/components/RichText/index.vue";
import { processHtmlForToc } from "@/utils/util";
import { MENU_TYPE, MENU_TYPE_MSG, ACTION_STATUS } from "@/consts/enum";

const props = defineProps({
  content: {
    type: String,
    default: ""
  },
  title: {
    type: String,
    default: ""
  },
  readonly: {
    type: Boolean,
    default: false
  }
});

const global = useGlobalStore();
const contentRef = ref();
const richTextRef = ref();
const [richTextHtml, setRichTextHtml] = useState("");
const [oldRichTextHtml, setOldRichTextHtml] = useState("");
const [anchorItems, setAnchorItems] = useState([]);
const [title, setTitle] = useState("");
const [oldTitle, setOldTitle] = useState("");
const [status, setStatus] = useState(ACTION_STATUS.SAVE);

const handleInit = content => {
  const { anchorItems, processedHtml } = processHtmlForToc(content);
  setRichTextHtml(processedHtml);
  setAnchorItems(anchorItems);
};

const handleUpdateContent = content => {
  const html = richTextRef.value?.getRichTextHtml();
  handleInit(content || html || oldRichTextHtml.value);
};

const hanldeClear = () => {
  setOldRichTextHtml("");
  setOldTitle("");
};

const handleEdit = () => {
  setStatus(ACTION_STATUS.EDIT);
  setOldRichTextHtml(richTextHtml.value);
  setOldTitle(title.value);
};

const handleSave = () => {
  handleUpdateContent();
  hanldeClear();
  setStatus(ACTION_STATUS.SAVE);
};

const handleCancel = () => {
  handleUpdateContent(oldRichTextHtml.value);
  setTitle(oldTitle.value);
  hanldeClear();
  setStatus(ACTION_STATUS.SAVE);
};

const handlePreview = () => {
  handleUpdateContent();

  setStatus(
    status.value === ACTION_STATUS.PREVIEW
      ? ACTION_STATUS.EDIT
      : ACTION_STATUS.PREVIEW
  );
};

watch(
  () => [props.content, props.title],
  ([content, nextTitle]) => {
    handleInit(content || "");
    setTitle(nextTitle || "");
  },
  {
    immediate: true
  }
);
</script>

<style lang="less" scoped>
.preview {
  display: flex;
  height: 100%;
  background: #fff;
  padding: 24px;
  .preview-content {
    flex: 1;
    padding: 0 48px 0 24px;
    overflow: hidden;
    overflow-y: auto;
    &-title {
      margin-bottom: 24px;
      padding-bottom: 28px;
      background: url("@/assets/img/title_boder@2x.png") no-repeat left bottom;
      background-size: 100% 5px;
      /* &::after {
        content: "";
        display: block;
        width: 100%;
        height: 1px;
        background: #e7e9e8;
      } */
    }
    &-title-editor {
      border: none !important;
      box-shadow: none !important;
      color: var(--rich-text-content-color);
      font-size: 2em;
      font-weight: bold;
      padding:0 0 28px;
    }
  }
  .preview-sidebar {
    width: 300px;
    height: 100%;
    &-toc {
      overflow: hidden;
      overflow-y: auto;
      height: calc(100% - 48px);
    }

    &-tools {
      height: 48px;
      padding-left: 12px;
      display: flex;
      align-items: center;
    }
  }
}
</style>
