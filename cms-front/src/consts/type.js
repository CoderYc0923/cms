// 上传文件业务类型枚举
export const UPLOAD_FILE_TYPE = {
  DOUYIN_ORDER: "dy_order",
  DOUYIN_COMMENT: "dy_comment",

  QW_COMMENT: "qw_comment",

  POINT_ORDER: "point_order", // 指定订单号
  POINT_MOBILE: "point_mobile", // 指定号码
  KUAISHOU_ORDER: "ks_order", // 订单
  KUAISHOU_COMMENT: "ks_comment", // 评价
  KUAISHOU_MANUAL_PHONE: "ks_manualPhone", // 指定号码发送-手动输入
  KUAISHOU_IMPORT_PHONE: "ks_designationPhone", // 指定号码发送-文件导入

  ADD_FANS: "add_fans", // 自动加粉-上传二维码
  SHORT_URL: "shorturl", // 短链欢迎语图片
  KF_Avatar: "kf_avatar", // 微信客服
  SHORT_IMGS: "shortimgs", // 微信客服短链

  MINI_APP_MANAGEMENT: "mini_app_management", //小程序管理

  QUALIFICATION_MANAGEMENT: "qualification_management", //资质管理
};
