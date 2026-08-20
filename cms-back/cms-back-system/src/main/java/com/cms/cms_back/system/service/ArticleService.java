package com.cms.cms_back.system.service;

import com.cms.cms_back.pojo.dto.article.CreateArticleDTO;
import com.cms.cms_back.pojo.dto.article.SaveArticleDTO;
import com.cms.cms_back.pojo.entity.Article;
import com.cms.cms_back.pojo.vo.article.GetArticleVO;

public interface ArticleService {

    GetArticleVO getArticle(Long nodeId);

    GetArticleVO getPublicArticle(String slug, Long nodeId);

    Article getArticleByNodeId(Long nodeId);
    
    void create(CreateArticleDTO dto, Long userId);

    void save(Long nodeId, SaveArticleDTO dto, Long userId);

    void delete(Long id);

    void publish(Long nodeId, Long userId);

    void unpublish(Long nodeId, Long userId);
}
