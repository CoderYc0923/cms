package com.cms.cms_back.system.service.serviceImpl;

import org.springframework.stereotype.Service;

import com.cms.cms_back.system.mapper.ArticleMapper;
import com.cms.cms_back.system.service.ArticleService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.common.exception.ErrorCode;
import com.cms.cms_back.pojo.dto.article.CreateArticleDTO;
import com.cms.cms_back.pojo.entity.Article;
import com.cms.cms_back.pojo.enums.PublishStatus;

@Service
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;

    public ArticleServiceImpl(ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
    }

    @Override
    public void create(CreateArticleDTO dto) {
        if (hasArticle(dto.getNodeId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文章已存在");
        }

        Article article = new Article();
        article.setNodeId(dto.getNodeId());
        article.setContent(dto.getContent());
        article.setPublishStatus(PublishStatus.formCode(dto.getPublishStatus()));

        articleMapper.insert(article);
    }

    /**
     * 检查文章是否存在
     * 
     * @param nodeId
     * @return
     */
    private Boolean hasArticle(Long nodeId) {
        return getArticleByNodeId(nodeId) != null;
    }

    /**
     * 获取文章
     * 
     * @param nodeId
     * @return
     */
    private Article getArticleByNodeId(Long nodeId) {
        return articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getNodeId, nodeId)
                        .isNull(Article::getDeletedAt));
    }
}
