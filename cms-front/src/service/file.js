import request from '@/utils/request'
/**
 * 上传接口-导入文件
 * @param {String} type 业务类型（用来区分文件路径）
 * @param {*} file 文件内容
 * @returns {Object} fileName：文件名，path：文件路径
 */
export async function importFile({ type, accountId }, file) {
  return request(`/file/upload?businessType=${type}&accountId=${accountId}`, {
    headers: {
      "Content-Type": "multipart/form-data",
    },
    method: "POST",
    params: file,
  });
}
