<template>
  <div class="flex-box-row" style="height: 100%">
    <div class="flex-25">
      <Catalogue space-slug="iot" @articleClick="handleArticleClick" @nodeDeleted="handleNodeDeleted" />
    </div>
    <div class="flex-75">
      <Preview
        :content="content"
        :title="title"
        :node-id="currentNodeId"
        :publish-status="publishStatus"
        :node-sort="currentNodeSort"
        space-slug="iot"
        :space-id="2"
        v-show="hasArticle"
        @saved="handleArticleSaved"
      />
      <Empty v-show="!hasArticle" />
    </div>
  </div>
</template>

<script setup>
import Catalogue from '@/components/Catalogue/index.vue'
import Preview from '@/components/Preview/index.vue'
import Empty from '@/components/Empty/index.vue'
import { MENU_TYPE } from '@/consts/enum'
import { getArticle } from '@/service/items'

const [content, setContent] = useState('')
const [title, setTitle] = useState('')
const [hasArticle, setHasArticle] = useState(false)
const [currentNodeId, setCurrentNodeId] = useState(null)
const [currentNodeSort, setCurrentNodeSort] = useState(null)
const [publishStatus, setPublishStatus] = useState('draft')

const handleArticleClick = node => {
  handleGetArticle(node)
}

const handleGetArticle = async node => {
  if (!node?.nodeId) {
    return
  }
  try {
    const res = await getArticle(node.nodeId)
    if (res.code === 0 || res.code === 200) {
      const data = res.data || {}
      setCurrentNodeId(node.nodeId)
      setCurrentNodeSort(node.sort ?? 0)
      setPublishStatus(data.publishStatus || 'draft')
      setTitle(node.title || '')
      setContent(data.content || '')
      setHasArticle(true)
    }
  } catch (error) {
    setCurrentNodeId(null)
    setCurrentNodeSort(null)
    setPublishStatus('draft')
    setHasArticle(false)
    setTitle('')
    setContent('')
  }
}

const handleArticleSaved = ({ content, title: nextTitle }) => {
  setContent(content)
  setTitle(nextTitle)
}

const handleNodeDeleted = ({ nodeId, type }) => {
  if (type === MENU_TYPE.ARTICLE && currentNodeId.value === nodeId) {
    setCurrentNodeId(null)
    setCurrentNodeSort(null)
    setPublishStatus('draft')
    setHasArticle(false)
    setTitle('')
    setContent('')
  }
}
</script>

<style lang="less" scoped>
</style>
