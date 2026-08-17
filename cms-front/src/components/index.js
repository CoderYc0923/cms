import { defineAsyncComponent } from 'vue'
export const components = app => {
  const requiredComponent = import.meta.glob('./*/*.vue')
  Object.keys(requiredComponent).forEach(fileName => {
    // fileName 同级目录所有的.vue文件
    const com = requiredComponent[fileName]
    //获取到的文件名替换掉不需要的字符
    const pathStrArr = fileName.replace('./', '').replace('.vue', '').split('/')
    //接收到的实例进行全局组件的挂载
    app.component(pathStrArr[1] === 'index' ? pathStrArr[0] : pathStrArr[1], defineAsyncComponent(com))
  })
}
