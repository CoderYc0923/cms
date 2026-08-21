package com.cms.cms_back.pojo.dto.mq;

import java.io.Serializable;

import com.cms.cms_back.pojo.enums.PublishEventType;

import lombok.Data;

/**
 * 发布事件消息体
 * PublishEventsMessage
 */
@Data
public class PublishEventsMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long nodeId;

    private Long userId;

    private PublishEventType eventType;

    private String payload;
}
