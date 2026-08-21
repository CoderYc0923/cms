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
@RocketMQMessageListener(topic = ArticleNodeConstant.TOPIC, consumerGroup = ArticleNodeConstant.GROUP, selectorExpression = ArticleNodeConstant.CREATE_TAG)
public class CreateArticleNodeConsumer implements RocketMQListener<ArticleNodeMessage> {

    private static final Logger log = LoggerFactory.getLogger(CreateArticleNodeConsumer.class);

    private final ArticleService articleService;

    public CreateArticleNodeConsumer(ArticleService articleService) {
        this.articleService = articleService;
    }

    @Override
    public void onMessage(ArticleNodeMessage message) {
        if (message == null || message.getNodeId() == null || message.getUserId() == null) {
            log.error("消费创建文章节点消息失败,非法消息体, message: {}", message);
            return;
        }

        CreateArticleDTO articleDTO = new CreateArticleDTO();
        articleDTO.setNodeId(message.getNodeId());

        articleService.create(articleDTO, message.getUserId());
        log.info("消费创建文章节点消息，nodeId：{}, userId: {}", message.getNodeId(), message.getUserId());
    }
}
