import { defineStore } from 'pinia'
import { getSpaceList } from '@/service/space'

export const useSpacesStore = defineStore('spaces', {
  state: () => ({
    list: [],
    loaded: false
  }),
  getters: {
    enabledList (state) {
      return state.list.filter(item => item.status === 1)
    },
    slugIdMap (state) {
      return Object.fromEntries(state.list.map(item => [item.slug, item.id]))
    }
  },
  actions: {
    async fetchList (status) {
      const res = await getSpaceList(status)
      if (res.code === 0 || res.code === 200) {
        this.list = res.data || []
        this.loaded = true
      }
      return this.list
    },
    getSpaceId (slug) {
      return this.slugIdMap[slug] ?? null
    },
    getSpaceName (slug) {
      return this.list.find(item => item.slug === slug)?.name || slug
    }
  }
})
