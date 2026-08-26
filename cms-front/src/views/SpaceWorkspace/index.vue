<template>
  <div class="space-workspace">
    <aside class="space-workspace__sidebar">
      <Catalogue
        ref="catalogueRef"
        :space-slug="spaceSlug"
        @articleClick="handleArticleClick"
        @nodeDeleted="handleNodeDeleted"
      />
    </aside>
    <section class="space-workspace__main">
      <Preview
        v-show="hasArticle"
        :content="content"
        :title="title"
        :node-id="currentNodeId"
        :publish-status="publishStatus"
        :node-sort="currentNodeSort"
        :space-slug="spaceSlug"
        :space-id="spaceId"
        @saved="handleArticleSaved"
        @publishStatusChange="handlePublishStatusChange"
        @published="handlePublished"
      />
      <Empty v-show="!hasArticle" />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import Catalogue from '@/shared/components/Catalogue/index.vue'
import Preview from '@/shared/components/Preview/index.vue'
import Empty from '@/shared/components/Empty/index.vue'
import { useArticleWorkspace } from '@/hooks/useArticleWorkspace'
import { useSpacesStore } from '@/stores/spaces'

const props = defineProps({
  spaceSlug: {
    type: String,
    default: ''
  }
})

const route = useRoute()
const spacesStore = useSpacesStore()
const catalogueRef = ref(null)

const spaceSlug = computed(() =>
  props.spaceSlug || route.meta.spaceSlug || route.params.spaceSlug || ''
)

const spaceId = computed(() => spacesStore.getSpaceId(spaceSlug.value))

const {
  content,
  title,
  hasArticle,
  currentNodeId,
  currentNodeSort,
  publishStatus,
  handleArticleClick,
  handleArticleSaved,
  handlePublishStatusChange,
  handleNodeDeleted,
  resetArticle
} = useArticleWorkspace()

const refreshTree = () => {
  catalogueRef.value?.refresh?.()
}

const handlePublished = () => {
  refreshTree()
}

onMounted(async () => {
  if (!spacesStore.loaded) {
    await spacesStore.fetchList()
  }
})

watch(spaceSlug, () => {
  resetArticle()
})
</script>

<style scoped lang="less">
.space-workspace {
  display: flex;
  height: 100%;
  min-height: 0;

  &__sidebar {
    flex: 0 0 var(--sidebar-width);
    width: var(--sidebar-width);
    min-width: 0;
    height: 100%;
    background: var(--color-bg-surface);
  }

  &__main {
    flex: 1;
    min-width: 0;
    height: 100%;
    background: var(--color-bg-surface);
  }
}
</style>
