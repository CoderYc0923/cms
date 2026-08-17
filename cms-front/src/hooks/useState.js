import { cloneDeep } from 'lodash'

/**
 * 使用useState函数定义一个名为useState的函数
 * @param {any} initialState - 初始状态值
 * @returns {Array} - 返回一个包含[state, setState]的数组
 */
const useState = (initialState) => {
  const state = ref(cloneDeep(initialState))
  const setState = (newState) => {
    state.value = cloneDeep(newState)
  }
  return [state, setState]
}

export default useState