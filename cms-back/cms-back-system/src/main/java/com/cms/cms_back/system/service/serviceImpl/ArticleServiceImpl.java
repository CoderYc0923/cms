package com.cms.cms_back.system.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.cms_back.system.mapper.ArticleMapper;
import com.cms.cms_back.system.mapper.NodeMapper;
import com.cms.cms_back.system.mapper.SpaceMapper;
import com.cms.cms_back.system.mq.producers.PublishEventsProducer;
import com.cms.cms_back.system.service.ArticleService;

import tools.jackson.databind.ObjectMapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.common.exception.ErrorCode;
import com.cms.cms_back.pojo.dto.article.CreateArticleDTO;
import com.cms.cms_back.pojo.dto.article.SaveArticleDTO;
import com.cms.cms_back.pojo.dto.mq.PublishEventsMessage;
import com.cms.cms_back.pojo.entity.Article;
import com.cms.cms_back.pojo.entity.Node;
import com.cms.cms_back.pojo.entity.Space;
import com.cms.cms_back.pojo.enums.PublishEventType;
import com.cms.cms_back.pojo.enums.PublishStatus;
import com.cms.cms_back.pojo.vo.article.GetArticleVO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final NodeMapper nodeMapper;
    private final SpaceMapper spaceMapper;
    private final ObjectMapper objectMapper;
    private final PublishEventsProducer publishEventsProducer;

    private static final Logger log = LoggerFactory.getLogger(ArticleServiceImpl.class);

    public ArticleServiceImpl(ArticleMapper articleMapper, NodeMapper nodeMapper, SpaceMapper spaceMapper,
            ObjectMapper objectMapper, PublishEventsProducer publishEventsProducer) {
        this.articleMapper = articleMapper;
        this.nodeMapper = nodeMapper;
        this.spaceMapper = spaceMapper;
        this.objectMapper = objectMapper;
        this.publishEventsProducer = publishEventsProducer;
    }

    /**
     * 获取文章
     */
    @Override
    public GetArticleVO getArticle(Long nodeId) {
        if (nodeId == null || nodeId <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文章节点ID不能为空");
        }

        Article article = getArticleByNodeId(nodeId);

        return toVO(article);
    }

    /**
     * 获取公开文章
     */
    @Override
    public GetArticleVO getPublicArticle(String slug, Long nodeId) {
        if (nodeId == null || nodeId <= 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "文章不存在");
        }

        Space space = spaceMapper.selectOne(
                new LambdaQueryWrapper<Space>()
                        .eq(Space::getSlug, slug));
        if (space == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文章不存在");
        }

        Article article = getArticleByNodeId(nodeId);
        if (article == null
                || !space.getId().equals(article.getSpaceId())
                || article.getPublishStatus() != PublishStatus.PUBLISHED) {
            throw new BizException(ErrorCode.NOT_FOUND, "文章不存在");
        }

        return toVO(article);
    }

    /**
     * 创建文章
     */
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

        try {
            articleMapper.insert(article);
        } catch (DuplicateKeyException e) {
            log.info("文章已存在, nodeId: {}", dto.getNodeId());
        }
    }

    /**
     * 保存文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(Long nodeId, SaveArticleDTO dto, Long userId) {
        if (nodeId == null || nodeId <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文章节点ID不能为空");
        }

        Article article = getArticleByNodeId(nodeId);
        if (article == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文章不存在");
        }

        LambdaUpdateWrapper<Article> updateWrapper = new LambdaUpdateWrapper<Article>()
                .eq(Article::getNodeId, nodeId)
                .isNull(Article::getDeletedAt)
                .set(Article::getContent, dto.getContent());

        articleMapper.update(null, updateWrapper);

        changeArticlePublishStatus(nodeId, PublishStatus.formCode(dto.getPublishStatus()), userId);
    }

    /**
     * 删除文章
     */
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
                .set(Article::getDeletedAt, LocalDateTime.now()));
    }

    /**
     * 发布文章
     * 
     * @param nodeId
     * @param userId
     */
    @Override
    public void publish(Long nodeId, Long userId) {
        changeArticlePublishStatus(nodeId, PublishStatus.PUBLISHED, userId);
    }

    /**
     * 取消发布文章
     */
    @Override
    public void unpublish(Long nodeId, Long userId) {
        changeArticlePublishStatus(nodeId, PublishStatus.DRAFT, userId);
    }

    /**
     * 改变文章发布状态
     * 
     * @param nodeId
     * @param publishStatus
     * @param userId
     */
    private void changeArticlePublishStatus(Long nodeId, PublishStatus publishStatus, Long userId) {
        if (nodeId == null || nodeId <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文章节点ID不能为空");
        }

        Article article = getArticleByNodeId(nodeId);
        if (article == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文章不存在");
        }

        boolean isPublished = article.getPublishStatus() == PublishStatus.PUBLISHED;

        LocalDateTime publishAt = publishStatus == PublishStatus.DRAFT ? article.getPublishAt()
                : (isPublished ? article.getPublishAt() : LocalDateTime.now());

        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getNodeId, nodeId)
                .isNull(Article::getDeletedAt)
                .set(Article::getPublishStatus, publishStatus)
                .set(Article::getPublishAt, publishAt)
                .set(Article::getUpdatedBy, userId));

        sendPublishEvents(nodeId, publishStatus, isPublished, userId, article);

    }

    private void sendPublishEvents(Long nodeId, PublishStatus publishStatus, boolean isPublished, Long userId,
            Article article) {

        PublishEventType eventType = PublishEventType.PUBLISHED;
        if (publishStatus == PublishStatus.DRAFT) {
            eventType = PublishEventType.UNPUBLISHED;
        } else {
            eventType = isPublished ? PublishEventType.UPDATED : PublishEventType.PUBLISHED;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("nodeId", nodeId);
        payload.put("userId", userId);
        String content = null;
        if (article.getContent() != null) {
            int len = Math.min(article.getContent().length(), 20);
            content = article.getContent().substring(0, len);
        }
        payload.put("content", content);

        String payloadJson = toJson(payload);

        String eventId = UUID.randomUUID().toString().replace("-", "");

        PublishEventsMessage message = new PublishEventsMessage();
        message.setNodeId(nodeId);
        message.setUserId(userId);
        message.setEventId(eventId);
        message.setEventType(eventType);
        message.setPayload(payloadJson);

        publishEventsProducer.publishAfterCommit(message);
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

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw e;
        }
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
