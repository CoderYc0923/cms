import { ERROR_CODE_TO_LOGIN } from '@/consts/codeEnum'
import notification from 'ant-design-vue/es/notification'
import { useUserStore } from '@/stores/user'

const statusMessage = {
  200: '服务器成功返回请求的数据。',
  201: '新建或修改数据成功。',
  202: '一个请求已经进入后台排队（异步任务）。',
  204: '删除数据成功。',
  400: '发出的请求有错误，服务器没有进行新建或修改数据的操作。',
  401: '登录超时',
  403: '用户得到授权，但是访问是被禁止的。',
  404: '发出的请求针对的是不存在的记录，服务器没有进行操作。',
  406: '请求的格式不可得。',
  410: '请求的资源被永久删除，且不会再得到的。',
  422: '当创建一个对象时，发生一个验证错误。',
  500: '服务器发生错误，请检查服务器。',
  502: '网关错误。',
  503: '服务不可用，服务器暂时过载或维护。',
  504: '网关超时。'
}

export function checkStatus(response) {
  if (!response) {
    notification.error({
      message: '请求错误'
    })
    return
  }
  if (response.code >= 200 && response.code < 300) {
    return response
  }
  const errortext = statusMessage[response.code] || response.statusText
  notification.error({
    message: `请求错误 ${response.code}`,
    description: errortext
  })
  if (ERROR_CODE_TO_LOGIN.includes(response.code)) {
    useUserStore().resetAuth()
  }
}

const codeMessage = {
  10001: '用户未登录'
}

export function checkCode(params) {
  const errortext = codeMessage[params.code] || params.msg
  notification.error({
    message: '操作错误',
    description: errortext
  })
}
