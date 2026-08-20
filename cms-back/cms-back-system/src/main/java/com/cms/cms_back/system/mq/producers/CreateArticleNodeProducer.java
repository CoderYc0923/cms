package com.cms.cms_back.system.mq.producers;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    public void publishAfterCommit(ArticleNodeMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    publish(message);
                }
            });
            return;
        }

        publish(message);
    }

    public void publish(ArticleNodeMessage message) {
        try {
            String destination = ArticleNodeConstant.TOPIC + ":" + ArticleNodeConstant.CREATE_TAG;
            rocketMQTemplate.convertAndSend(destination, message);
            log.info("发送创建文章节点消息成功，nodeId：{}", message.getNodeId());
        } catch (Exception e) {
            log.error("发送创建文章节点消息失败，nodeId：{}", message.getNodeId(), e);
            throw e;
        }
    }
}
