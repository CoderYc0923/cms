package com.cms.cms_back.system.service.serviceImpl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.cms.cms_back.system.mapper.ArticleMapper;
import com.cms.cms_back.system.mapper.NodeMapper;
import com.cms.cms_back.system.service.ArticleService;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.common.exception.ErrorCode;
import com.cms.cms_back.pojo.dto.article.CreateArticleDTO;
import com.cms.cms_back.pojo.entity.Article;
import com.cms.cms_back.pojo.entity.Node;
import com.cms.cms_back.pojo.enums.PublishStatus;
import com.cms.cms_back.pojo.vo.article.GetArticleVO;

@Service
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final NodeMapper nodeMapper;

    public ArticleServiceImpl(ArticleMapper articleMapper, NodeMapper nodeMapper) {
        this.articleMapper = articleMapper;
        this.nodeMapper = nodeMapper;
    }

    @Override
    public GetArticleVO getArticle(Long nodeId) {
        if (nodeId == null || nodeId <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文章节点ID不能为空");
        }

        Article article = getArticleByNodeId(nodeId);

        return toVO(article);
    }

    @Override
    public void create(CreateArticleDTO dto, Long userId) {
        if (hasArticle(dto.getNodeId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文章已存在");
        }

        Article article = new Article();
        article.setNodeId(dto.getNodeId());
        article.setContent(dto.getContent());
        article.setPublishStatus(PublishStatus.formCode(dto.getPublishStatus()));

        Node node = getNodeByNodeId(dto.getNodeId());
        if (node == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文章节点不存在");
        }
        article.setSpaceId(node.getSpaceId());

        article.setCreatedBy(userId);

        articleMapper.insert(article);
    }

    @Override
    public void delete(Long nodeId) {
        if (nodeId == null || nodeId <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文章节点ID不能为空");
        }

        Article article = getArticleByNodeId(nodeId);
        if (article == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文章不存在");
        }

        if (article.getPublishStatus() == PublishStatus.PUBLISHED) {
            throw new BizException(ErrorCode.CONFLICT, "已发布文章不能删除");
        }

        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
            .eq(Article::getNodeId, nodeId)
            .set(Article::getDeletedAt, LocalDateTime.now())
        );
    }

    private GetArticleVO toVO(Article article) {
        if (article == null) {
            return null;
        }

        return GetArticleVO.builder()
                .id(article.getId())
                .nodeId(article.getNodeId())
                .content(article.getContent())
                .publishStatus(article.getPublishStatus().getCode())
                .publishAt(article.getPublishAt())
                .createdAt(article.getCreatedAt())
                .updatedAt(article.getUpdatedAt())
                .createdBy(article.getCreatedBy())
                .updatedBy(article.getUpdatedBy())
                .build();
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
    @Override
    public Article getArticleByNodeId(Long nodeId) {
        if (nodeId == null || nodeId <= 0) {
            return null;
        }
        return articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getNodeId, nodeId)
                        .isNull(Article::getDeletedAt));
    }

    /**
     * 获取节点
     */
    private Node getNodeByNodeId(Long nodeId) {
        if (nodeId == null) {
            return null;
        }

        return nodeMapper.selectOne(
                new LambdaQueryWrapper<Node>()
                        .eq(Node::getId, nodeId)
                        .isNull(Node::getDeletedAt));
    }
}
