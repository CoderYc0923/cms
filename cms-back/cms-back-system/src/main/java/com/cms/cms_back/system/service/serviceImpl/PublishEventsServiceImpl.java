package com.cms.cms_back.system.service.serviceImpl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.common.exception.ErrorCode;
import com.cms.cms_back.pojo.entity.Article;
import com.cms.cms_back.pojo.entity.PublishEvent;
import com.cms.cms_back.pojo.enums.PublishEventType;
import com.cms.cms_back.system.mapper.ArticleMapper;
import com.cms.cms_back.system.mapper.PublishEventMapper;
import com.cms.cms_back.system.service.PublishEventsService;

@Service
public class PublishEventsServiceImpl implements PublishEventsService {

    private final PublishEventMapper publishEventMapper;
    private final ArticleMapper articleMapper;

    public PublishEventsServiceImpl(PublishEventMapper publishEventMapper, ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
        this.publishEventMapper = publishEventMapper;
    }

    @Override
    public void createEvent(Long nodeId, Long userId, PublishEventType eventType, String payload) {

        if (nodeId == null || nodeId <= 0) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "nodeId is required");
        }

        Article article = getArticleByNodeId(nodeId);
        if (article == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "article not found");
        }

        PublishEvent publishEvent = new PublishEvent();
        publishEvent.setArticleId(article.getId());
        publishEvent.setSpaceId(article.getSpaceId());
        publishEvent.setEventType(eventType);
        publishEvent.setOccurredAt(LocalDateTime.now());
        publishEvent.setPayload(payload);

        publishEventMapper.insert(publishEvent);

    }

    private Article getArticleByNodeId(Long nodeId) {
        return articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getNodeId, nodeId)
                        .isNull(Article::getDeletedAt));
    }
}
