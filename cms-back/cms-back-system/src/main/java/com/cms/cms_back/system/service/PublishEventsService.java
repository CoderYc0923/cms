package com.cms.cms_back.system.service;

import com.cms.cms_back.pojo.enums.PublishEventType;

public interface PublishEventsService {

    void createEvent(Long nodeId, Long userId, PublishEventType eventType, String payload);
}
