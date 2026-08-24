/**
 * slug → spaceId，与 db/changelog/003-add-two-spaces.sql 种子数据一致。
 * 若库中 id 不同，请同步修改。
 */
export const SPACE_SLUG_ID_MAP = {
  shopchup: 1,
  iot: 2
}

export const DEFAULT_BIZ_TYPE = 'article_richtext'
