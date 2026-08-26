<template>
  <div class="rich-text">
    <Toolbar
      :editor="editorRef"
      :defaultConfig="toolbarConfig"
      :mode="mode"
      class="rich-text_toolbar"
    />
    <Editor
      v-model="textContent"
      :defaultConfig="editorConfig"
      :mode="mode"
      @onCreated="handleCreated"
      @onChange="handleChange"
      class="rich-text_editor yuque-article preview-style"
    />

    <a-modal
      v-model:open="uploadModalVisible"
      title="正在上传"
      :footer="null"
      :closable="false"
      :mask-closable="false"
      centered
      width="420px"
    >
      <div class="rich-text_upload-panel">
        <div class="rich-text_upload-name" :title="uploadFileName">
          {{ uploadFileName }}
        </div>
        <a-progress :percent="uploadProgress" status="active" />
        <a-button
          block
          danger
          class="rich-text_upload-cancel"
          :loading="uploadCancelling"
          @click="handleCancelUpload"
        >
          取消上传
        </a-button>
      </div>
    </a-modal>
  </div>
</template>
<script setup>
import "@wangeditor/editor/dist/css/style.css";
import "@/assets/style/resetEditor.less";
import { Editor, Toolbar } from "@wangeditor/editor-for-vue";
import { message } from "ant-design-vue";
import { uploadToOss, UploadAbortError } from "@/utils/ossUpload";
import { rewriteAdminFileUrls } from "@/utils/fileUrl";
import { registerEditorMenus } from "./registerEditorMenus";

registerEditorMenus();

const props = defineProps({
  id: {
    type: Number,
    default: undefined,
  },
  content: {
    type: String,
    default: ""
  },
  spaceId: {
    type: Number,
    default: undefined
  },
  spaceSlug: {
    type: String,
    default: ""
  }
});

const mode = "default"; // 或 'simple'
const toolbarConfig = {
  excludeKeys: ["fullScreen", "insertImage"],
  modalAppendToBody: true,
};
const editorConfig = {
  placeholder: "请输入文章内容...",
  MENU_CONF: {},
  // 粘贴截图时走 OSS，避免 base64 直插（不会调后端）
  customPaste: (editor, event) => {
    const items = event.clipboardData?.items;
    if (!items?.length) {
      return true;
    }
    const imageItem = Array.from(items).find((item) =>
      item.type?.startsWith("image/")
    );
    if (!imageItem) {
      return true;
    }
    const file = imageItem.getAsFile();
    if (!file) {
      return true;
    }
    event.preventDefault();
    handleOssUpload(
      file,
      (url, alt, href) => {
        editor.dangerouslyInsertHtml(
          `<img src="${url}" alt="${alt || ""}" href="${href || url}" />`
        );
      },
      "image"
    ).catch(() => {});
    return false;
  },
  hoverbarKeys: {
    image: {
      menuKeys: ["editImageSize", "deleteImage"],
    },
    video: {
      menuKeys: ["editVideoSize"],
    },
  },
};

// 编辑器实例，必须用 shallowRef
const editorRef = shallowRef();
const [textContent, setTextContent] = useState("<p>请输入文章内容...</p>");
const [uploadModalVisible, setUploadModalVisible] = useState(false);
const [uploadProgress, setUploadProgress] = useState(0);
const [uploadFileName, setUploadFileName] = useState("");
const [uploadCancelling, setUploadCancelling] = useState(false);
const cancelUploadTask = shallowRef(null);

const resetUploadModal = () => {
  setUploadModalVisible(false);
  setUploadProgress(0);
  setUploadFileName("");
  setUploadCancelling(false);
  cancelUploadTask.value = null;
};

const handleCancelUpload = async () => {
  if (!cancelUploadTask.value || uploadCancelling.value) {
    return;
  }

  setUploadCancelling(true);
  try {
    await cancelUploadTask.value();
    message.info("已取消上传");
  } catch (error) {
    console.error("cancel upload failed", error);
    message.error("取消上传失败");
  } finally {
    resetUploadModal();
  }
};

const handleOssUpload = async (file, insertFn, type) => {
  const editor = editorRef.value;

  setUploadFileName(file.name);
  setUploadProgress(0);
  setUploadCancelling(false);
  setUploadModalVisible(true);

  const { promise, cancel } = uploadToOss(file, {
    spaceId: props.spaceId,
    spaceSlug: props.spaceSlug,
    onProgress: (progress) => {
      setUploadProgress(progress);
      editor?.showProgressBar?.(progress);
    },
  });

  cancelUploadTask.value = cancel;

  try {
    const stableUrl = await promise;
    editor?.showProgressBar?.(100);
    const previewUrl = rewriteAdminFileUrls(stableUrl);

    if (type === "video") {
      insertFn(previewUrl, "");
      return;
    }

    insertFn(previewUrl, file.name, previewUrl);
  } catch (error) {
    if (error instanceof UploadAbortError || error?.name === "UploadAbortError") {
      return;
    }
    console.error("upload failed", error);
    message.error(error?.message || "上传失败");
    throw error;
  } finally {
    resetUploadModal();
  }
};

// wangEditor 要求在编辑器创建前完成 MENU_CONF 配置，创建后再赋值无效
const initUpload = () => {
  editorConfig.MENU_CONF["uploadImage"] = {
    fieldName: `${props.id}-image`,
    // 与后端 oss.max-image-bytes 对齐（10MB）
    maxFileSize: 10 * 1024 * 1024,
    // 最多可上传几个文件，默认为 100
    maxNumberOfFiles: 100,
    // 选择文件时的类型限制，默认为 ['image/*'] 。如不想限制，则设置为 []
    allowedFileTypes: ["image/*"],
    // 一律走 OSS，不设 base64 直插（否则小图不会调接口）
    base64LimitSize: 0,
    onError(file, err, res) {
      console.log(`${file.name} 上传出错`, err, res);
    },
    async customUpload(file, insertFn) {
      await handleOssUpload(file, insertFn, "image");
    },
  };

  editorConfig.MENU_CONF["uploadVideo"] = {
    fieldName: `${props.id}-video`,
    // 与后端 oss.max-video-bytes 对齐（500MB，走分片）
    maxFileSize: 500 * 1024 * 1024,
    // 最多可上传几个文件，默认为 5
    maxNumberOfFiles: 5,
    // 选择文件时的类型限制，默认为 ['video/*'] 。如不想限制，则设置为 []
    allowedFileTypes: ["video/*"],
    onError(file, err, res) {
      console.log(`${file.name} 上传出错`, err, res);
    },
    async customUpload(file, insertFn) {
      await handleOssUpload(file, insertFn, "video");
    },
  };
};

initUpload();

const getRichTextHtml = () => {
  //const html = editorRef.value?.getHtml()
  return textContent.value;
};

const handleCreated = (editor) => {
  editorRef.value = editor; // 记录 editor 实例，重要！
};

const handleChange = (editor) => {
  console.log("handleChange", editor.children);
};

watch(
  () => props.content,
  content => {
    if (content) {
      nextTick(() => {
        setTextContent(content);
      });
    }
  },
  { immediate: true }
);

onBeforeUnmount(() => {
  const editor = editorRef.value;
  editor && editor.destroy();
});

defineExpose({ getRichTextHtml });
</script>
<style lang="less" scoped>
.rich-text {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
  overflow: hidden;

  &_toolbar {
    position: relative;
    z-index: 30;
    flex-shrink: 0;
    min-height: 40px;
    border-bottom: 1px solid var(--color-border);
    background: var(--color-bg-surface);
    overflow: visible;

    :deep(.w-e-toolbar) {
      overflow: visible;
      flex-wrap: wrap;
    }

    :deep(.w-e-drop-panel),
    :deep(.w-e-select-list),
    :deep(.w-e-bar-item-menus-container) {
      z-index: 31;
    }
  }

  &_editor {
    flex: 1;
    min-height: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;

    :deep(.w-e-text-container) {
      flex: 1;
      min-height: 0;
      height: auto !important;
      display: flex;
      flex-direction: column;
    }

    :deep(.w-e-scroll) {
      flex: 1;
      min-height: 0;
      height: auto !important;
      overflow-y: auto !important;
      -webkit-overflow-scrolling: touch;
    }

    :deep([data-slate-editor]) {
      min-height: 100%;
      padding: 12px 0 24px;
    }
  }

  &_upload-panel {
    padding-top: 4px;
  }

  &_upload-name {
    margin-bottom: 12px;
    color: var(--color-text-secondary);
    font-size: var(--text-body);
    word-break: break-all;
  }

  &_upload-cancel {
    margin-top: 16px;
  }
}

:global(.cms-image-size-modal) {
  padding: 4px 2px 0;
}

:global(.cms-image-size-form) {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

:global(.cms-image-size-field) {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

:global(.cms-image-size-label) {
  font-size: 13px;
  color: var(--color-text-secondary);
}

:global(.cms-image-size-input) {
  width: 100%;
  height: 32px;
  padding: 0 10px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font-size: 14px;
  outline: none;
  box-sizing: border-box;

  &:focus {
    border-color: var(--color-primary);
  }
}

:global(.cms-image-size-hint) {
  margin: 0;
  font-size: 12px;
  color: var(--color-text-tertiary);
  line-height: 18px;
}

:global(.cms-image-size-ok) {
  display: block;
  width: 100%;
  margin-top: 16px;
  padding: 8px 0;
  border: none;
  border-radius: 6px;
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  cursor: pointer;

  &:hover {
    background: var(--color-primary-hover);
  }
}
</style>
