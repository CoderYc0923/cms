package com.cms.cms_back.system.mq.consumers;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cms.cms_back.pojo.constants.mq.consumer.PublishEventsConstant;
import com.cms.cms_back.pojo.dto.mq.PublishEventsMessage;
import com.cms.cms_back.system.service.PublishEventsService;

/**
 * 发布事件消息消费者
 * PublishEventsConsumer
 */
@Component
@RocketMQMessageListener(topic = PublishEventsConstant.TOPIC, consumerGroup = PublishEventsConstant.GROUP)
public class PublishEventsConsumer implements RocketMQListener<PublishEventsMessage> {

    private static final Logger log = LoggerFactory.getLogger(PublishEventsConsumer.class);

    private final PublishEventsService publishEventsService;

    public PublishEventsConsumer(PublishEventsService publishEventsService) {
        this.publishEventsService = publishEventsService;
    }

    @Override
    public void onMessage(PublishEventsMessage message) {
        if (message == null) {
            log.error("消费发布事件消息失败,消息体为空");
            throw new RuntimeException("事件消息体为空");
        }

        if (message.getNodeId() == null || message.getUserId() == null || message.getEventType() == null) {
            log.error("消费发布事件消息失败,消息体参数异常，nodeId：{}, userId: {}, eventType: {}", message.getNodeId(),
                    message.getUserId(), message.getEventType());
        }

        try {
            publishEventsService.createEvent(message.getNodeId(), message.getUserId(), message.getEventType(),
                    message.getPayload());
            log.info("消费发布事件消息成功,nodeId：{}, userId: {}, eventType: {}", message.getNodeId(), message.getUserId(),
                    message.getEventType());
        } catch (RuntimeException e) {
            log.error("消费发布事件消息失败,message：{}", message, e);
            throw e;
        }

    }
}
