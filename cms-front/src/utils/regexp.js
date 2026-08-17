/**
 * 手机号码简单校验
 * @description
 * @type {RegExp}
 */
export const samplePhoneRegexp = /^1[3456789]\d{9}$/
/**
 * 手机号码
 * @description
 * @type {RegExp}
 */
export const phoneRegexp = /^1(?:3\d|4[4-9]|5[0-35-9]|6[67]|7[013-8]|8\d|9\d)\d{8}$/
/**
 * 电信号段
 * @description
 * @type {RegExp}
 */
export const chinanetRegexp = /^((133)|(149)|(153)|(18[0|1|9])|(17[3|4|7])|(199))[\d]{8}$/
/**
 * 联通号段
 * @description
 * @type {RegExp}
 */
export const unicomRegexp = /^((13[0-2])|(14[5|6])|(15[5-6])|(166)|(17[1|5-6])|(18[5-6]))[\d]{8}$/
/**
 * 移动号段
 * @description
 * @type {RegExp}
 */
export const cmccRegexp = /^((13[4-9])|(14[7-8])|(15([0-2]|[7-9]))|(17[2|8])|(18[2|3|4|7|8])|(19[8]))[\d]{8}$/
/**
 * 邮箱地址
 * @description 大小写字母数子下划线（_）和连接符（-）--- @ ---
 *              大小写字母数子下划线（_）和连接符（-）--- . ---
 *              大小写字母数子下划线（_）和连接符（-）
 * @type {RegExp}
 */
export const emailRegexp = /^([a-zA-Z0-9._-])+@([a-zA-Z0-9_-])+(\.[a-zA-Z0-9_-])+/

export const otherEmailRegexp = /^\w{0,64}@([A-Za-z0-9][-A-Za-z0-9]+\.)+[A-Za-z]{2,255}$/
/**
 * 固定电话
 */
export const telephoneRegexp = /^0(?:[3-9]\d{2}|0852|0853|0886|2\d|10)(-)?\d{7,8}$/
/**
 * 税务号
 */
export const taxIdRegexp = /^[0-9a-zA-Z]{15,20}$/
/**
 * qq
 */
export const qqRegexp = /[0-9]{1,15}/
/**
 * 银行开户行账户 数字或者字母
 */
export const accountNoRegexp = /^[a-zA-Z0-9]+$/

/**
 * 密码
 */
export const passwordRegexp = /^[a-zA-Z0-9]{6,16}$/

/**
 * 密码验证
 */
export const otherPasswordRegexp = /^(?!([a-zA-Z]+|\d+)$)[a-zA-Z\d]{8,32}$/

/**
 * 短信签名
 */
export const signatureRegexp = /^[a-zA-Z0-9\u4e00-\u9fa5]{2,20}$/

export const specialCharacters = '[`~!@#$^&*()=|{}\':;\',\\\\[\\\\].<>/?~！@#￥……&*（）——|{}【】‘；：”“\'。，、？]'
/**
 * url
 */
export const urlRegexp = new RegExp('^(http[s]{0,1})://[a-zA-Z0-9\\.\\-]+\\.([a-zA-Z0-9]{2,4})(:\\d+)?(/[a-zA-Z0-9\\.\\-~!@#$%^&amp;*+?:_/=<>]*)?', 'g')

/**
 * 匹配所有链接（包括带有 http、https 的链接以及不带协议的短链接）
 */
export const linkRegexp = /(?:https?|ftp):\/\/[a-zA-Z0-9.-]+(?:\.[a-zA-Z]{2,6})?(?:\/[^\s]*)?|www\.[a-zA-Z0-9.-]+(?:\/[^\s]*)?|\b[a-zA-Z0-9.-]+\.[a-zA-Z]{2,6}\/[a-zA-Z0-9._-]+/g

/**
 * 社会信用码
 */
export const creditCodeRegexp = /^[0-9A-HJ-NPQRTUWXY]{2}\d{6}[0-9A-HJ-NPQRTUWXY]{10}$/

/**
 * 用于提取模板变量
 */
export const sliceTplVariable = /\${[\d\w]{0,}\}/g

/**
 * 身份证正则
 */
export const idRegex = {
    chinaMainland: /(^\d{15}$)|(^\d{17}([0-9]|X)$)/, // 大陆身份证
    hongKong: /^[A-Z]\d{6}\([0-9A]\)$/, // 香港身份证
    taiwan: /^[A-Z][12]\d{8}$/, // 台湾身份证
    macao: /^[1|5|7]\d{6}\(\d\)$/, // 澳门身份证
}

/**
 * 英文字母（大小写）和数字
 */
export const alphanumericRegexp = /^[a-zA-Z0-9]+$/
