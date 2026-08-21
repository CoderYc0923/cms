package com.cms.cms_back.system.service;
import com.cms.cms_back.pojo.dto.mq.PublishEventsMessage;


public interface PublishEventsService {

    void createEvent(PublishEventsMessage message);
}
