package com.cms.cms_back.system.mq.producers;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cms.cms_back.pojo.constants.mq.consumer.ArticleNodeConstant;
import com.cms.cms_back.pojo.dto.mq.ArticleNodeMessage;

/**
 * 创建文章节点消息生产者
 * CreateArticleNodeProducer
 */
@Component
public class CreateArticleNodeProducer {

    private final RocketMQTemplate rocketMQTemplate;

    private static final Logger log = LoggerFactory.getLogger(CreateArticleNodeProducer.class);

    public CreateArticleNodeProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public void publish(ArticleNodeMessage message) {
        try {
            String destination = ArticleNodeConstant.TOPIC + ":" + ArticleNodeConstant.CREATE_TAG;
            rocketMQTemplate.convertAndSend(destination, message);
        } catch (Exception e) {
            log.error("发送创建文章节点消息失败，nodeId：{}", message.getNodeId(), e);
            throw e;
        }
    }
}
