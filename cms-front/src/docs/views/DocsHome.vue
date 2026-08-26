<template>
  <div class="docs-home">
    <aside class="docs-home__sidebar">
      <Catalogue
        readonly
        :space-slug="docsSpace"
        :initial-node-id="initialNodeId"
        @articleClick="handleArticleClick"
      />
    </aside>
    <section class="docs-home__content">
      <Preview
        v-show="hasArticle"
        :content="content"
        :title="title"
        readonly
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

const docsSpace = import.meta.env.VITE_DOCS_SPACE || 'unknown'
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
  if (!node?.nodeId || !docsSpace) {
    return
  }
  try {
    const res = await getPublicArticle(docsSpace, node.nodeId)
    if (res.code === 0 || res.code === 200) {
      const data = res.data || {}
      setTitle(node.title || '')
      setContent(data.content || '')
      setHasArticle(true)
    }
  } catch (error) {
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
.docs-home {
  display: flex;
  height: 100%;

  &__sidebar {
    flex: 0 0 25%;
    width: 25%;
    min-width: 240px;
    max-width: 320px;
    border-right: 1px solid var(--cms-color-border);
    background: var(--cms-color-surface);
    overflow: hidden;
  }

  &__content {
    flex: 1;
    min-width: 0;
    overflow: hidden;
    background: #fff;
  }
}
</style>
