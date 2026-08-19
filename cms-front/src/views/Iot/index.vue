<template>
  <div class="flex-box-row" style="height: 100%">
    <div class="flex-25">
      <Catalogue @articleClick="handleArticleClick" @nodeDeleted="handleNodeDeleted" />
    </div>
    <div class="flex-75">
      <Preview :content="content" :title="title" v-show="hasArticle" />
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
      setTitle(node.title || '')
      setContent(data.content || '')
      setHasArticle(true)
    }
  } catch (error) {
    setCurrentNodeId(null)
    setHasArticle(false)
    setTitle('')
    setContent('')
  }
}

const handleNodeDeleted = ({ nodeId, type }) => {
  if (type === MENU_TYPE.ARTICLE && currentNodeId.value === nodeId) {
    setCurrentNodeId(null)
    setHasArticle(false)
    setTitle('')
    setContent('')
  }
}
</script>

<style lang="less" scoped>
</style>
