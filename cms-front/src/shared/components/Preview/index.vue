<template>

  <div
    class="preview"
    :class="{
      'preview--readonly': readonly,
      'preview--editing': isEditing
    }"
  >

    <header v-if="!readonly" class="preview-toolbar">

      <div class="preview-toolbar__meta">

        <span

          class="preview-toolbar__status"

          :class="{ 'is-published': isPublished }"

        >{{ publishStatusLabel }}</span>

      </div>

      <div class="preview-toolbar__actions">

        <template v-if="status === ACTION_STATUS.SAVE">

          <button type="button" class="preview-btn preview-btn--ghost" @click="handleEdit">

            编辑

          </button>

          <button

            v-if="!isPublished"

            type="button"

            class="preview-btn preview-btn--primary"

            :disabled="publishing"

            @click="handlePublish"

          >

            {{ publishing ? '发布中…' : '发布' }}

          </button>

          <button

            v-else

            type="button"

            class="preview-btn preview-btn--ghost"

            :disabled="publishing"

            @click="handleUnpublish"

          >

            {{ publishing ? '处理中…' : '下架' }}

          </button>

        </template>

        <template v-else>

          <button

            type="button"

            class="preview-btn preview-btn--ghost"

            @click="handlePreview"

          >

            {{ status === ACTION_STATUS.PREVIEW ? '继续编辑' : '预览' }}

          </button>

          <button

            type="button"

            class="preview-btn preview-btn--primary"

            :disabled="saving"

            @click="handleSave"

          >

            {{ saving ? '保存中…' : '保存' }}

          </button>

          <button type="button" class="preview-btn preview-btn--text" @click="handleCancel">

            取消

          </button>

        </template>

      </div>

    </header>



    <div v-if="isEditing" class="preview-edit">
      <div class="preview-edit__head">
        <a-textarea
          v-model:value="title"
          auto-size
          class="preview-title preview-title--editor"
          placeholder="无标题"
        />
      </div>
      <RichText
        ref="richTextRef"
        class="preview-edit__editor"
        :content="richTextHtml"
        :id="nodeId"
        :space-id="spaceId"
        :space-slug="spaceSlug"
      />
    </div>

    <div v-else ref="contentRef" class="preview-scroll hide-scrollbar">
      <div class="preview-canvas">
        <article class="preview-main">
          <h1 class="preview-title">{{ title }}</h1>
          <div class="yuque-article" v-html="richTextHtml" />
        </article>

        <aside v-if="showToc" class="preview-toc hide-scrollbar">
          <div class="preview-toc__head">本页目录</div>
          <Anchor
            :anchor-items="anchorItems"
            :container="getScrollContainer"
            :scroll-offset="72"
          />
        </aside>
      </div>
    </div>
  </div>
</template>



<script setup>

import Anchor from '@/shared/components/Anchor/index.vue'

import RichText from '@/shared/components/RichText/index.vue'

import { message } from 'ant-design-vue'

import { processHtmlForToc } from '@/utils/util'

import { rewriteAdminFileUrls, toPublicFileUrl } from '@/utils/fileUrl'

import { saveArticle, editItem, publishArticle, unpublishArticle } from '@/service/items'

import { ACTION_STATUS, PUBLISH_STATUS, PUBLISH_STATUS_MSG } from '@/consts/enum'



const EMPTY_EDITOR_HTML = '<p>请输入文章内容...</p>'



const props = defineProps({

  content: {

    type: String,

    default: ''

  },

  title: {

    type: String,

    default: ''

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

    default: ''

  },

  publishStatus: {

    type: String,

    default: 'draft'

  },

  nodeSort: {

    type: Number,

    default: undefined

  }

})



const emit = defineEmits(['saved', 'publishStatusChange', 'published'])



const contentRef = ref()

const richTextRef = ref()



const getScrollContainer = () => contentRef.value

const [richTextHtml, setRichTextHtml] = useState('')

const [oldRichTextHtml, setOldRichTextHtml] = useState('')

const [anchorItems, setAnchorItems] = useState([])

const [title, setTitle] = useState('')

const [oldTitle, setOldTitle] = useState('')

const [status, setStatus] = useState(ACTION_STATUS.SAVE)

const [saving, setSaving] = useState(false)

const [publishing, setPublishing] = useState(false)

const [localPublishStatus, setLocalPublishStatus] = useState(PUBLISH_STATUS.DRAFT)



const publishStatusLabel = computed(() =>

  PUBLISH_STATUS_MSG[localPublishStatus.value] || localPublishStatus.value

)



const isPublished = computed(() => localPublishStatus.value === PUBLISH_STATUS.PUBLISHED)

const isEditing = computed(() => !props.readonly && status.value === ACTION_STATUS.EDIT)

const showToc = computed(() =>
  !isEditing.value && (anchorItems.value.length > 0 || props.readonly)
)



const handleInit = content => {

  const normalizedContent = props.readonly ? content : rewriteAdminFileUrls(content)

  const { anchorItems: items, processedHtml } = processHtmlForToc(normalizedContent)

  setRichTextHtml(processedHtml)

  setAnchorItems(items)

}



const handleUpdateContent = content => {

  const html = richTextRef.value?.getRichTextHtml()

  const nextHtml = content || html || oldRichTextHtml.value

  handleInit(props.readonly ? nextHtml : toPublicFileUrl(nextHtml))

}



const hanldeClear = () => {

  setOldRichTextHtml('')

  setOldTitle('')

}



const handleEdit = () => {

  setStatus(ACTION_STATUS.EDIT)

  setOldRichTextHtml(richTextHtml.value)

  setOldTitle(title.value)

}



const handleSave = async () => {

  if (!props.nodeId) {

    message.error('未选择文章')

    return

  }



  const rawHtml = richTextRef.value?.getRichTextHtml() || ''

  const content = toPublicFileUrl(rawHtml.trim())



  if (!content || content === EMPTY_EDITOR_HTML) {

    message.warning('请输入文章内容')

    return

  }



  setSaving(true)

  try {

    await saveArticle(props.nodeId, {

      content,

      publishStatus: localPublishStatus.value || PUBLISH_STATUS.DRAFT

    })



    if (props.nodeSort != null && title.value !== oldTitle.value) {

      await editItem(

        {

          title: title.value,

          sort: props.nodeSort

        },

        props.nodeId

      )

    }



    handleUpdateContent(content)

    hanldeClear()

    setStatus(ACTION_STATUS.SAVE)

    message.success('保存成功')

    emit('saved', { content, title: title.value })

  } catch (error) {

    console.error('save article failed', error)

    message.error(error?.message || '保存失败')

  } finally {

    setSaving(false)

  }

}



const handleCancel = () => {

  handleUpdateContent(oldRichTextHtml.value)

  setTitle(oldTitle.value)

  hanldeClear()

  setStatus(ACTION_STATUS.SAVE)

}



const handlePreview = () => {

  handleUpdateContent()



  setStatus(

    status.value === ACTION_STATUS.PREVIEW ? ACTION_STATUS.EDIT : ACTION_STATUS.PREVIEW

  )

}



const handlePublish = async () => {

  if (!props.nodeId) {

    message.error('未选择文章')

    return

  }

  setPublishing(true)

  try {

    await publishArticle(props.nodeId)

    setLocalPublishStatus(PUBLISH_STATUS.PUBLISHED)

    emit('publishStatusChange', PUBLISH_STATUS.PUBLISHED)

    emit('published')

    message.success('发布成功')

  } catch (error) {

    message.error(error?.message || '发布失败')

  } finally {

    setPublishing(false)

  }

}



const handleUnpublish = async () => {

  if (!props.nodeId) {

    message.error('未选择文章')

    return

  }

  setPublishing(true)

  try {

    await unpublishArticle(props.nodeId)

    setLocalPublishStatus(PUBLISH_STATUS.DRAFT)

    emit('publishStatusChange', PUBLISH_STATUS.DRAFT)

    emit('published')

    message.success('已下架')

  } catch (error) {

    message.error(error?.message || '下架失败')

  } finally {

    setPublishing(false)

  }

}



watch(

  () => props.publishStatus,

  nextStatus => {

    setLocalPublishStatus(nextStatus || PUBLISH_STATUS.DRAFT)

  },

  { immediate: true }

)



watch(

  () => [props.content, props.title],

  ([content, nextTitle]) => {

    handleInit(content || '')

    setTitle(nextTitle || '')

  },

  {

    immediate: true

  }

)

</script>



<style lang="less" scoped>

.preview {

  display: flex;

  flex-direction: column;

  height: 100%;

  background: var(--color-bg-surface);



  &-toolbar {

    display: flex;

    align-items: center;

    justify-content: space-between;

    flex-shrink: 0;

    gap: 16px;

    min-height: 44px;

    padding: 0 24px;

    border-bottom: 1px solid var(--color-border-subtle);

    background: var(--color-bg-surface);



    &__meta {

      display: flex;

      align-items: center;

      gap: 8px;

    }



    &__status {

      display: inline-flex;

      align-items: center;

      padding: 2px 8px;

      border-radius: 4px;

      font-size: var(--text-caption);

      color: var(--color-text-secondary);

      background: var(--color-border-subtle);



      &.is-published {

        color: var(--color-success);

        background: rgba(0, 180, 42, 0.08);

      }

    }



    &__actions {

      display: flex;

      align-items: center;

      gap: 6px;

    }

  }



  &-scroll {

    flex: 1;

    min-height: 0;

    overflow-y: auto;

  }



  &-canvas {

    display: flex;

    align-items: flex-start;

    gap: 48px;

    max-width: var(--content-canvas-max-width);

    margin: 0 auto;

    padding: 40px 32px 80px;

  }



  &-main {

    flex: 1;

    min-width: 0;

    max-width: var(--content-max-width);

  }



  &-title {

    margin: 0 0 28px;

    padding: 0;

    border: none;

    font-size: 32px;

    font-weight: 700;

    line-height: 1.25;

    color: var(--color-text-primary);



    &--editor {

      margin-bottom: 28px;

      padding: 0 !important;

      border: none !important;

      border-radius: 0 !important;

      box-shadow: none !important;

      font-size: 32px;

      font-weight: 700;

      line-height: 1.25;

      color: var(--color-text-primary);

      resize: none;

    }

  }



  &-toc {

    position: sticky;

    top: 24px;

    flex-shrink: 0;

    width: var(--toc-width);

    max-height: calc(100vh - 120px);

    overflow-y: auto;

    padding-top: 8px;



    &__head {

      margin-bottom: 8px;

      padding: 0 10px;

      font-size: var(--text-caption);

      font-weight: 500;

      line-height: 20px;

      color: var(--color-text-tertiary);

      letter-spacing: 0.04em;

      text-transform: uppercase;

    }

  }



  &-btn {

    display: inline-flex;

    align-items: center;

    justify-content: center;

    padding: 6px 12px;

    border: 1px solid transparent;

    border-radius: var(--radius-sm);

    font-size: var(--text-label);

    line-height: 20px;

    cursor: pointer;

    transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;



    &:disabled {

      opacity: 0.6;

      cursor: not-allowed;

    }



    &--primary {

      border-color: var(--color-primary);

      background: var(--color-primary);

      color: #fff;



      &:hover:not(:disabled) {

        background: var(--color-primary-hover);

        border-color: var(--color-primary-hover);

      }

    }



    &--ghost {

      border-color: var(--color-border);

      background: var(--color-bg-surface);

      color: var(--color-text-primary);



      &:hover:not(:disabled) {

        background: var(--color-bg-hover);

      }

    }



    &--text {

      background: transparent;

      color: var(--color-text-secondary);



      &:hover:not(:disabled) {

        color: var(--color-text-primary);

        background: var(--color-bg-hover);

      }

    }

  }



  &--readonly {

    .preview-canvas {

      padding-top: 48px;

    }

  }

  &--editing {
    overflow: hidden;
  }

  &-edit {
    display: flex;
    flex-direction: column;
    flex: 1;
    min-height: 0;
    overflow: hidden;

    &__head {
      flex-shrink: 0;
      max-width: var(--content-canvas-max-width);
      width: 100%;
      margin: 0 auto;
      padding: 24px 32px 0;
    }

    &__editor {
      flex: 1;
      min-height: 0;
      max-width: var(--content-canvas-max-width);
      width: 100%;
      margin: 0 auto;
      padding: 0 32px 24px;
    }
  }

}

</style>

