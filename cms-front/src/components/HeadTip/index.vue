<template>
  <div class="container" :style="{ background: customBgc }">
    <div class="container_point" v-if="isDotIcon" />
    <my-icon class="container_icon" :type="`icon-icon_${iconType}`" v-if="needIcon && !isDotIcon"/>
    <ul :style="{ listStyle, paddingLeft: listStyle === 'none' && '0px' }" v-if="tipList.length > 0" class="container_text">
      <li class="container_text_item" :style="liStyle" v-for="(item, index) in tipList" :key="index">
        <span v-html="item.text"></span>
        <a-button v-if="item.button" class="container_text_item_button" type="link" @click="goTo(item)">{{ item.button }}</a-button>
      </li>
    </ul>
    <slot></slot>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  tipList: {
    type: Array,
    default: () => ([])
  },
  listStyle: {
    type: String,
    default: 'decimal'
  },
  customBgc: {
    type: String,
    default: 'rgba(213, 230, 255, 0.5)'
  },
  needIcon: {
    type: Boolean,
    default: true
  },
  isDotIcon: {
    type: Boolean,
    default: false
  },
  iconType: {
    type: String,
    default: 'remind'
  },
  liStyle: {
    type: String,
    default: ''
  },
})

const router = useRouter()

const goTo = (item) => {
  if (item.cb) item.cb()
  if (item.route) {
    router.push(item.route)
  }
  if (item.link) {
    window.open(item.link, '_blank')
  }
}
</script>

<style lang="less" scoped>
.container {
  display: flex;
  flex-wrap: nowrap;
  justify-content: flex-start;
  margin-bottom: 24px;
  padding: 8px 16px;
  border-radius: 2px;

  &_point {
    display: inline-block;
    width: 4px;
    height: 4px;
    border-radius: 50%;
    margin: 9px 9px 0 0;
    background-color: rgba(59,44,98, 0.65);
  }

  &_icon {
    transform: translateY(4px);
    color: #A77CFE;
    margin-right: 16px;
  }
  &_text {
    padding-left: 16px;
    list-style: decimal;
    font-size: 1rem;
    font-weight: 400;
    color: rgba(59,44,98, 0.65);
    line-height: 20px;
    margin-bottom: 0;
    &_item {
      font-size: 1rem;
      font-weight: 400;
      color: rgba(59,44,98, 0.65);
      line-height: 20px;
      &_button {
        padding: 0;
        height: 19px;
        color: #A77CFE !important;
        border-bottom: 1px solid;
        border-radius: 0;
        margin-left: 4px;
      }
    }
  }
}
</style>
