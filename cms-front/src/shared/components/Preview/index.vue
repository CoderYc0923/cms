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
      <div class="yuque-article preview-style" v-html="richTextHtml" v-show="status !== ACTION_STATUS.EDIT"></div>
      <RichText
        v-show="status === ACTION_STATUS.EDIT"
        :content="richTextHtml"
        ref="richTextRef"
        :id="nodeId"
        :space-id="spaceId"
        :space-slug="spaceSlug"
      />
    </div>
    <div class="preview-sidebar">
      <div class="preview-sidebar-tools" v-if="!readonly">
        <a-tag :color="isPublished ? 'success' : 'default'">{{ publishStatusLabel }}</a-tag>
        <template v-if="status === ACTION_STATUS.SAVE">
          <a-button type="primary" @click="handleEdit">编辑</a-button>
          <a-button
            v-if="!isPublished"
            class="no-first-btn"
            type="primary"
            ghost
            :loading="publishing"
            @click="handlePublish"
          >发布</a-button>
          <a-button
            v-else
            class="no-first-btn"
            :loading="publishing"
            @click="handleUnpublish"
          >下架</a-button>
        </template>
        <template v-else>
          <a-button
            type="primary"
            @click="handlePreview"
          >{{ status === ACTION_STATUS.PREVIEW ? '取消预览' : '预览' }}</a-button>
          <a-button
            type="primary"
            class="no-first-btn"
            :loading="saving"
            @click="handleSave"
          >保存</a-button>
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
import Anchor from '@/shared/components/Anchor/index.vue'
import RichText from '@/shared/components/RichText/index.vue'
import { message } from "ant-design-vue";
import { processHtmlForToc } from "@/utils/util";
import { rewriteAdminFileUrls, toPublicFileUrl } from "@/utils/fileUrl";
import { saveArticle, editItem, publishArticle, unpublishArticle } from "@/service/items";
import { MENU_TYPE, MENU_TYPE_MSG, ACTION_STATUS, PUBLISH_STATUS, PUBLISH_STATUS_MSG } from "@/consts/enum";

const EMPTY_EDITOR_HTML = "<p>请输入文章内容...</p>";

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
  },
  nodeId: {
    type: Number,
    default: undefined
  },
  spaceId: {
    type: Number,
    default: undefined
  },
  spaceSlug: {
    type: String,
    default: ""
  },
  publishStatus: {
    type: String,
    default: "draft"
  },
  nodeSort: {
    type: Number,
    default: undefined
  }
});

const emit = defineEmits(["saved", "publishStatusChange", "published"]);

const global = useGlobalStore();
const contentRef = ref();
const richTextRef = ref();
const [richTextHtml, setRichTextHtml] = useState("");
const [oldRichTextHtml, setOldRichTextHtml] = useState("");
const [anchorItems, setAnchorItems] = useState([]);
const [title, setTitle] = useState("");
const [oldTitle, setOldTitle] = useState("");
const [status, setStatus] = useState(ACTION_STATUS.SAVE);
const [saving, setSaving] = useState(false);
const [publishing, setPublishing] = useState(false);
const [localPublishStatus, setLocalPublishStatus] = useState(PUBLISH_STATUS.DRAFT);

const publishStatusLabel = computed(() =>
  PUBLISH_STATUS_MSG[localPublishStatus.value] || localPublishStatus.value
);

const isPublished = computed(() => localPublishStatus.value === PUBLISH_STATUS.PUBLISHED);

const handleInit = content => {
  const normalizedContent = props.readonly
    ? content
    : rewriteAdminFileUrls(content);
  const { anchorItems, processedHtml } = processHtmlForToc(normalizedContent);
  setRichTextHtml(processedHtml);
  setAnchorItems(anchorItems);
};

const handleUpdateContent = content => {
  const html = richTextRef.value?.getRichTextHtml();
  const nextHtml = content || html || oldRichTextHtml.value;
  handleInit(props.readonly ? nextHtml : toPublicFileUrl(nextHtml));
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

const handleSave = async () => {
  if (!props.nodeId) {
    message.error("未选择文章");
    return;
  }

  const rawHtml = richTextRef.value?.getRichTextHtml() || "";
  const content = toPublicFileUrl(rawHtml.trim());

  if (!content || content === EMPTY_EDITOR_HTML) {
    message.warning("请输入文章内容");
    return;
  }

  setSaving(true);
  try {
    await saveArticle(props.nodeId, {
      content,
      publishStatus: localPublishStatus.value || PUBLISH_STATUS.DRAFT
    });

    if (
      props.nodeSort != null &&
      title.value !== oldTitle.value
    ) {
      await editItem(
        {
          title: title.value,
          sort: props.nodeSort
        },
        props.nodeId
      );
    }

    handleUpdateContent(content);
    hanldeClear();
    setStatus(ACTION_STATUS.SAVE);
    message.success("保存成功");
    emit("saved", { content, title: title.value });
  } catch (error) {
    console.error("save article failed", error);
    message.error(error?.message || "保存失败");
  } finally {
    setSaving(false);
  }
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

const handlePublish = async () => {
  if (!props.nodeId) {
    message.error("未选择文章");
    return;
  }
  setPublishing(true);
  try {
    await publishArticle(props.nodeId);
    setLocalPublishStatus(PUBLISH_STATUS.PUBLISHED);
    emit("publishStatusChange", PUBLISH_STATUS.PUBLISHED);
    emit("published");
    message.success("发布成功");
  } catch (error) {
    message.error(error?.message || "发布失败");
  } finally {
    setPublishing(false);
  }
};

const handleUnpublish = async () => {
  if (!props.nodeId) {
    message.error("未选择文章");
    return;
  }
  setPublishing(true);
  try {
    await unpublishArticle(props.nodeId);
    setLocalPublishStatus(PUBLISH_STATUS.DRAFT);
    emit("publishStatusChange", PUBLISH_STATUS.DRAFT);
    emit("published");
    message.success("已下架");
  } catch (error) {
    message.error(error?.message || "下架失败");
  } finally {
    setPublishing(false);
  }
};

watch(
  () => props.publishStatus,
  nextStatus => {
    setLocalPublishStatus(nextStatus || PUBLISH_STATUS.DRAFT);
  },
  { immediate: true }
);

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
      min-height: 48px;
      padding-left: 12px;
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
    }
  }
}
</style>
