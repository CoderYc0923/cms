package com.cms.cms_back.system.service.serviceImpl;

import java.time.LocalDateTime;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.common.exception.ErrorCode;
import com.cms.cms_back.pojo.dto.mq.PublishEventsMessage;
import com.cms.cms_back.pojo.entity.Article;
import com.cms.cms_back.pojo.entity.PublishEvent;
import com.cms.cms_back.system.mapper.ArticleMapper;
import com.cms.cms_back.system.mapper.PublishEventMapper;
import com.cms.cms_back.system.service.PublishEventsService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PublishEventsServiceImpl implements PublishEventsService {

    private final PublishEventMapper publishEventMapper;
    private final ArticleMapper articleMapper;

    private static final Logger log = LoggerFactory.getLogger(PublishEventsServiceImpl.class);

    public PublishEventsServiceImpl(PublishEventMapper publishEventMapper, ArticleMapper articleMapper) {
        this.articleMapper = articleMapper;
        this.publishEventMapper = publishEventMapper;
    }

    @Override
    public void createEvent(PublishEventsMessage message) {

        if (message == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "message is required");
        }

        if (message.getNodeId() == null || message.getNodeId() <= 0) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "nodeId is required");
        }

        Article article = getArticleByNodeId(message.getNodeId());
        if (article == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "article not found");
        }

        PublishEvent publishEvent = new PublishEvent();
        publishEvent.setArticleId(article.getId());
        publishEvent.setSpaceId(article.getSpaceId());
        publishEvent.setEventId(message.getEventId());
        publishEvent.setEventType(message.getEventType());
        publishEvent.setOccurredAt(LocalDateTime.now());
        publishEvent.setPayload(message.getPayload());

        try {
            publishEventMapper.insert(publishEvent);
        } catch (DuplicateKeyException e) {
            log.info("发布事件已存在, eventId: {}", message.getEventId());
        }

    }

    private Article getArticleByNodeId(Long nodeId) {
        return articleMapper.selectOne(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getNodeId, nodeId)
                        .isNull(Article::getDeletedAt));
    }
}
