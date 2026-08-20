package com.cms.cms_back.system.mq.consumers;

import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import com.cms.cms_back.pojo.constants.mq.consumer.ArticleNodeConstant;
import com.cms.cms_back.pojo.dto.mq.ArticleNodeMessage;

@Component
@RocketMQMessageListener(
    topic = ArticleNodeConstant.TOPIC,
    consumerGroup = ArticleNodeConstant.GROUP,
    selectorExpression = ArticleNodeConstant.CREATE_TAG
)
public class CreateArticleNodeConsumer implements RocketMQListener<ArticleNodeMessage> {

}
