<template>
  <DocsViewer :space-slug="docsSpace" :embed="embedMode" />
</template>

<script setup>
import DocsViewer from '@/shared/components/DocsViewer/index.vue'
import { isEmbedMode } from '@/docs/utils/embed'

const docsSpace = import.meta.env.VITE_DOCS_SPACE || 'unknown'
const embedMode = isEmbedMode()
</script>
