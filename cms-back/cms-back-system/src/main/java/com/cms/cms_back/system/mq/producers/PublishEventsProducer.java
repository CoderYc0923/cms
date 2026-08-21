package com.cms.cms_back.system.mq.producers;

import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.cms.cms_back.pojo.constants.mq.consumer.PublishEventsConstant;
import com.cms.cms_back.pojo.dto.mq.PublishEventsMessage;

@Component
public class PublishEventsProducer {

    private static final Logger log = LoggerFactory.getLogger(PublishEventsProducer.class);

    private final RocketMQTemplate rocketMQTemplate;

    public PublishEventsProducer(RocketMQTemplate rocketMQTemplate) {
        this.rocketMQTemplate = rocketMQTemplate;
    }

    public void publishAfterCommit(PublishEventsMessage message) {
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

    public void publish(PublishEventsMessage message) {
        try {
            rocketMQTemplate.convertAndSend(PublishEventsConstant.TOPIC, message);
            log.info("发送发布事件消息成功，nodeId={}, articleId={}, eventType={}", message.getNodeId(), message.getArticleId(),
                    message.getEventType());
        } catch (Exception e) {
            log.error("发送发布事件消息失败，nodeId={}, articleId={}, eventType={}", message.getNodeId(), message.getArticleId(),
                    message.getEventType(), e);

            throw e;
        }
    }

}
