package com.cms.cms_back.system.service;

import java.util.List;
import java.util.Set;

import com.cms.cms_back.pojo.entity.Article;

public interface ArticleMediaRefService {

    /** 获取某篇文章引用的所有文件ID */
    List<Long> getFileIdsByArticleId(Long articleId);

    /** 删除某篇文章引用的所有文件引用关系 */
    void deleteRefsByArticleId(Long articleId);

    /** 根据正文 diff 引用关系，返回受影响的fileId */
    Set<Long> syncRefsByContent(Long articleId, Long spaceId, String content);

    /** 按照引用表 + 规则重算文件访问级别 */
    void recomputeAccessLevel(Set<Long> fileIds);

    /** 重算某篇文章当前引用的文件的访问级别 */
    void recomputeAccessLevelForArticle(Long articleId);

    /** 重算某篇文章受影响的文件访问级别 */
    void recomputeAccessLevelForArticleDiff(Article article);
}
