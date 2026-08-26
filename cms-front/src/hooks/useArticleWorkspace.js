import { ref } from 'vue'
import { MENU_TYPE } from '@/consts/enum'
import { getArticle } from '@/service/items'

export function useArticleWorkspace () {
  const content = ref('')
  const title = ref('')
  const hasArticle = ref(false)
  const currentNodeId = ref(null)
  const currentNodeSort = ref(null)
  const publishStatus = ref('draft')

  const resetArticle = () => {
    currentNodeId.value = null
    currentNodeSort.value = null
    publishStatus.value = 'draft'
    hasArticle.value = false
    title.value = ''
    content.value = ''
  }

  const loadArticle = async node => {
    if (!node?.nodeId) {
      return
    }
    try {
      const res = await getArticle(node.nodeId)
      if (res.code === 0 || res.code === 200) {
        const data = res.data || {}
        currentNodeId.value = node.nodeId
        currentNodeSort.value = node.sort ?? 0
        publishStatus.value = data.publishStatus || 'draft'
        title.value = node.title || ''
        content.value = data.content || ''
        hasArticle.value = true
      }
    } catch (error) {
      resetArticle()
    }
  }

  const handleArticleClick = node => {
    loadArticle(node)
  }

  const handleArticleSaved = ({ content: nextContent, title: nextTitle }) => {
    content.value = nextContent
    title.value = nextTitle
  }

  const handlePublishStatusChange = status => {
    publishStatus.value = status
  }

  const handleNodeDeleted = ({ nodeId, type }) => {
    if (type === MENU_TYPE.ARTICLE && currentNodeId.value === nodeId) {
      resetArticle()
    }
  }

  return {
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
    resetArticle,
    loadArticle
  }
}
