
/**
 * 
 * @param {Function} requestFunc 请求函数 
 * @param {Object} params 请求函数所需参数
 * @param  {Object} args 额外参数 requestSuccFunc：请求成功后执行的函数， finallyFunc：请求完成后执行的函数，catchFunc：捕获异常后执行的函数
 * @returns 
 */
const useRequest = async (requestFunc, params, ...args) => {
  let requestSuccFunc, finallyFunc, catchFunc
  args && args.length && args.forEach(item => {
    if (typeof item === 'object') {
      item['requestSuccFunc'] && (requestSuccFunc = item['requestSuccFunc'])
      item['finallyFunc'] && (finallyFunc = item['finallyFunc'])
      item['catchFunc'] && (catchFunc = item['catchFunc'])
    }
  })
  try {
    const res = await requestFunc(params, ...args)
    if (!res.code) {
      requestSuccFunc && requestSuccFunc()
      return {
        data: res.data,
        code: res.code
      }
    }
  } catch (error) {
    console.error(error)
    catchFunc && catchFunc()
    return {
      code: 9999,
      errorCode: error.code,
      msg: error.msg
    }
  } finally {
    finallyFunc && finallyFunc()
  }
}

export default useRequest
