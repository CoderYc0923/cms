package com.cms.cms_back.system.service;

import com.cms.cms_back.pojo.dto.article.CreateArticleDTO;
import com.cms.cms_back.pojo.vo.article.GetArticleVO;

public interface ArticleService {

    GetArticleVO getArticle(Long nodeId);
    
    void create(CreateArticleDTO dto, Long userId);
}
