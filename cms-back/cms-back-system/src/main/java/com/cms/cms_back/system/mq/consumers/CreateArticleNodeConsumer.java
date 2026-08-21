package com.cms.cms_back.system.mq.consumers;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cms.cms_back.pojo.constants.mq.consumer.ArticleNodeConstant;
import com.cms.cms_back.pojo.dto.article.CreateArticleDTO;
import com.cms.cms_back.pojo.dto.mq.ArticleNodeMessage;
import com.cms.cms_back.system.service.ArticleService;

@Component
@RocketMQMessageListener(
    topic = ArticleNodeConstant.TOPIC,
    consumerGroup = ArticleNodeConstant.GROUP,
    selectorExpression = ArticleNodeConstant.CREATE_TAG
)
public class CreateArticleNodeConsumer implements RocketMQListener<ArticleNodeMessage> {

    private static final Logger log = LoggerFactory.getLogger(CreateArticleNodeConsumer.class);

    private final ArticleService articleService;

    public CreateArticleNodeConsumer(ArticleService articleService) {
        this.articleService = articleService;
    }

    @Override
    public void onMessage(ArticleNodeMessage message) {
        if (message == null) {
            log.error("消费创建文章节点消息失败,消息体为空");
            throw new RuntimeException("消息体为空");
        }
        if (message.getNodeId() == null || message.getUserId() == null) {
            log.error("消费创建文章节点消息失败,消息体参数异常，nodeId：{}, userId: {}, message: {}", message.getNodeId(), message.getUserId(), message);
        }

        try {
            createDraftArticle(message.getNodeId(), message.getUserId());
            log.info("消费创建文章节点消息，nodeId：{}, userId: {}", message.getNodeId(), message.getUserId());
        } catch (RuntimeException e) {
            log.error("消费创建文章节点消息失败，nodeId：{}, userId: {}", message.getNodeId(), message.getUserId(), e);
            throw e;
        }
    }

    private void createDraftArticle(Long nodeId, Long userId) {
        CreateArticleDTO articleDTO = new CreateArticleDTO();
        articleDTO.setNodeId(nodeId);

        articleService.create(articleDTO, userId);
    }
}
