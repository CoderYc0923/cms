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
      class="rich-text_editor preview-style"
    />
  </div>
</template>
<script setup>
import "@wangeditor/editor/dist/css/style.css";
import "@/assets/style/resetEditor.less";
import { Editor, Toolbar } from "@wangeditor/editor-for-vue";

const props = defineProps({
  id: {
    type: Number,
    default: undefined,
  },
  content: {
    type: String,
    default: ""
  }
});

const mode = "default"; // 或 'simple'
const toolbarConfig = {
  excludeKeys: ["fullScreen"],
};
const editorConfig = {
  placeholder: "请输入文章内容...",
  MENU_CONF: {},
  hoverbarKeys: {
    image: {
      menuKeys: [
        "imageWidth30",
        "imageWidth50",
        "imageWidth100",
        //"imageSize",
        "editImage",
        //"viewImageLink",
        "deleteImage",
      ],
    },
  },
};

// 编辑器实例，必须用 shallowRef
const editorRef = shallowRef();
const [textContent, setTextContent] = useState("<p>请输入文章内容...</p>");

const initUpload = () => {
  editorConfig.MENU_CONF["uploadImage"] = {
    fieldName: `${props.id}-image`,
    // 单个文件的最大体积限制，默认为 2M
    maxFileSize: 2 * 1024 * 1024, // 2M
    // 最多可上传几个文件，默认为 100
    maxNumberOfFiles: 100,
    // 选择文件时的类型限制，默认为 ['image/*'] 。如不想限制，则设置为 []
    allowedFileTypes: ["image/*"],
    // 小于该值就插入 base64 格式（而不上传），默认为 0
    base64LimitSize: 5 * 1024, // 5kb
    onError(file, err, res) {
      console.log(`${file.name} 上传出错`, err, res);
    },
    // 自定义上传
    async customUpload(file, insertFn) {
      // TS 语法
      // async customUpload(file, insertFn) {                   // JS 语法
      // file 即选中的文件
      // 自己实现上传，并得到图片 url alt href
      // 最后插入图片
      insertFn(url, alt, href);
    },
  };

  editorConfig.MENU_CONF["uploadVideo"] = {
    fieldName: `${props.id}-video`,
    // 单个文件的最大体积限制，默认为 10M
    maxFileSize: 10 * 1024 * 1024, // 10M
    // 最多可上传几个文件，默认为 5
    maxNumberOfFiles: 5,
    // 选择文件时的类型限制，默认为 ['video/*'] 。如不想限制，则设置为 []
    allowedFileTypes: ["video/*"],
    onError(file, err, res) {
      console.log(`${file.name} 上传出错`, err, res);
    },
    // 自定义上传
    async customUpload(file, insertFn) {
      // TS 语法
      // async customUpload(file, insertFn) {                   // JS 语法
      // file 即选中的文件
      // 自己实现上传，并得到视频 url poster
      // 最后插入视频
      insertFn(url, poster);
    },
  };
};

const getRichTextHtml = () => {
  //const html = editorRef.value?.getHtml()
  return textContent.value;
};

const handleCreated = (editor) => {
  editorRef.value = editor; // 记录 editor 实例，重要！
  initUpload();
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
  border: 0px solid #ccc;

  &_toolbar {
    border-bottom: 2px solid rgba(5, 5, 5, 0.06);
  }

  &_editor {
    min-height: 500px;
    overflow-y: hidden;
  }
}
</style>
