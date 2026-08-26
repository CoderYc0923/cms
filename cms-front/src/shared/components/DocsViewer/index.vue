<template>
  <div class="docs-viewer" :class="{ 'docs-viewer--embed': embed }">
    <aside class="docs-viewer__sidebar">
      <Catalogue
        readonly
        :space-slug="spaceSlug"
        :initial-node-id="initialNodeId"
        @articleClick="handleArticleClick"
      />
    </aside>
    <section class="docs-viewer__content">
      <Preview
        v-show="hasArticle"
        readonly
        :content="content"
        :title="title"
      />
      <Empty v-show="!hasArticle" />
    </section>
  </div>
</template>

<script setup>
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import Catalogue from '@/shared/components/Catalogue/index.vue'
import Preview from '@/shared/components/Preview/index.vue'
import Empty from '@/shared/components/Empty/index.vue'
import { getPublicArticle } from '@/shared/api/public'

const props = defineProps({
  spaceSlug: {
    type: String,
    required: true
  },
  embed: {
    type: Boolean,
    default: false
  }
})

const route = useRoute()
const router = useRouter()

const [content, setContent] = useState('')
const [title, setTitle] = useState('')
const [hasArticle, setHasArticle] = useState(false)

const initialNodeId = computed(() => {
  const raw = route.params.nodeId
  if (!raw) {
    return undefined
  }
  const id = Number(raw)
  return Number.isFinite(id) ? id : undefined
})

const handleArticleClick = node => {
  if (!node?.nodeId) {
    return
  }
  router.replace({
    name: 'DocsArticle',
    params: { nodeId: node.nodeId }
  })
  handleGetArticle(node)
}

const handleGetArticle = async node => {
  if (!node?.nodeId || !props.spaceSlug) {
    return
  }
  try {
    const res = await getPublicArticle(props.spaceSlug, node.nodeId)
    if (res.code === 0 || res.code === 200) {
      const data = res.data || {}
      setTitle(node.title || '')
      setContent(data.content || '')
      setHasArticle(true)
    }
  } catch {
    setHasArticle(false)
    setTitle('')
    setContent('')
  }
}

watch(
  () => route.params.nodeId,
  nodeId => {
    if (!nodeId) {
      setHasArticle(false)
      setTitle('')
      setContent('')
    }
  }
)
</script>

<style scoped lang="less">
.docs-viewer {
  display: flex;
  height: 100%;
  min-height: 0;
  background: var(--color-bg-surface);

  &__sidebar {
    flex: 0 0 var(--sidebar-width);
    width: var(--sidebar-width);
    min-width: 0;
    height: 100%;
    background: var(--color-bg-surface);
    overflow: hidden;
  }

  &__content {
    flex: 1;
    min-width: 0;
    min-height: 0;
    height: 100%;
    overflow: hidden;
    background: var(--color-bg-surface);
  }

  &--embed {
    .docs-viewer__sidebar {
      flex-basis: 220px;
      width: 220px;
    }
  }
}
</style>
