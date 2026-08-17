<template>
  <div class="container" @click="handleClick">
    <a-popover>
      <template #content>
        <p
          :style="{
            width: `${props.popoverWidth}px` || 'unset',
            wordBreak: 'break-all',
          }"
          v-html="integrityContent"
        ></p>
      </template>
      <span ref="popTextRef" :class="{ clickStyle: canClick }" v-html="integrityContent"></span>
    </a-popover>
    <p
      v-show="!showPop"
      class="longerText"
      :class="['longerText', canClick ? 'clickStyle' : '']"
      ref="longerTextRef"
      v-html="integrityContent"
    ></p>
  </div>
</template>

<script setup>
import DOMPurify from 'dompurify';

const props = defineProps({
  content: { type: String },
  lineHeight: { type: Number, default: 30 },
  textWidth: { type: String, default: '240px' },
  lineCount: { type: Number, default: 2 },
  popoverWidth: { type: Number },
  propsShowPop: { type: Boolean },
  canClick: { type: Boolean },
});
const emits = defineEmits(['click'])

const showPop = ref(false);
const longerTextRef = ref();
const popTextRef = ref();

// 将content 内容 中的空格 替换成&nbsp; 并替换换行
const integrityContent = computed(() => {
  if (!props.content) return "";
   // 使用 DOMPurify 清理 HTML 内容,为防止XSS或者一些代码注入
  let cleanContent = DOMPurify.sanitize(props.content);
  // 将所有 HTML 标签转义
  cleanContent = cleanContent.replace(/</g, "&lt;").replace(/>/g, "&gt;");
  // 换行保留
  return cleanContent.replace(/\n/g, "<br>").replace(/\s/g, "&nbsp;");
});

const handleClick = () => {
  props.canClick && emits('click')
}

onMounted(() => {
  longerTextRef.value.style.width = `${props.textWidth}`;
  longerTextRef.value.style.lineHeight = `${props.lineHeight}px`;
  if (longerTextRef.value.offsetHeight > props.lineHeight * props.lineCount) {
    popTextRef.value.setAttribute(
      "style",
      `display:-webkit-box;
      overflow: hidden;
      text-overflow: ellipsis;
      width: ${props.textWidth};
      line-height: ${props.lineHeight}px;
      word-break: break-all;
      word-wrap: break-word;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: ${props.lineCount}`
    );
    showPop.value = true;
  } else {
    popTextRef.value.setAttribute("style", "display:none;");
    showPop.value = false;
  }
  //强制改成pop模式
  if (props.propsShowPop) {
    popTextRef.value.setAttribute(
      "style",
      `display:-webkit-box;
      overflow: hidden;
      text-overflow: ellipsis;
      width: ${props.textWidth};
      line-height: ${props.lineHeight}px;
      word-break: break-all;
      word-wrap: break-word;
      -webkit-box-orient: vertical;
      -webkit-line-clamp: ${props.lineCount};
      padding-top: 4px;`
    );
    showPop.value = true;
  }
});
</script>

<style lang="less" scoped>
.container {
  display: inline-block;
  .longerText {
    word-break: break-all;
  }
  .clickStyle {
    color: var(--highlight-color);
    cursor: pointer;
  }
}
</style>
