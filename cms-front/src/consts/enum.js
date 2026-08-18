export const COLOR = {
  SUCCESS: "rgba(33, 203, 121, 1)",
  FAILURE: "rgba(244, 102, 115, 1)",
  DEFAULT: "rgba(18, 18, 63, 0.45)",
  DEFAULT_DEEP: "rgba(18, 18, 63, 0.85)",
};

export const MENU_TYPE = {
  GROUP: 'group',
  MENU: 'menu',
  ARTICLE: 'article',
};

export const MENU_TYPE_MSG = {
  [MENU_TYPE.GROUP]: "分组",
  [MENU_TYPE.MENU]: "菜单",
  [MENU_TYPE.ARTICLE]: "文章",
};

export const NODE_STATUS = {
  OFFLINE: 0,
  ONLINE: 1,
};

export const NODE_STATUS_MSG = {
  [NODE_STATUS.ONLINE]: { text: "已上线", color: COLOR.SUCCESS },
  [NODE_STATUS.OFFLINE]: { text: "已下线", color: COLOR.FAILURE },
};

export const ACTION_TYPE = {
  ADD_CATALOGUE: 1, //新增目录
  DELETE_CATALOGUE: 2, //删除目录
  ADD_ARTICLE: 3, //新增文章
  EDIT_ARTICLE: 4, //编辑文章
  DELETE_ARTICLE: 5,//删除文章
  PREVIEW_ARTICLE: 6, //预览文章
}

export const ACTION_STATUS = {
  EDIT: 0,
  PREVIEW: 1,
  SAVE: 2,
}

export const CATALOGUE_MODE = {
  EDIT: 0,
  PREVIEW: 1,
}