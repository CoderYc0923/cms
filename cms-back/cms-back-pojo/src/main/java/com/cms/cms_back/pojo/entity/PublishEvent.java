package com.cms.cms_back.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cms.cms_back.pojo.enums.PublishEventType;

import lombok.Data;

@TableName("publish_events")
@Data
public class PublishEvent {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Long spaceId;

    private PublishEventType eventType;

    private LocalDateTime occurredAt;

    /** JSON 摘要，先按字符串存 */
    private String payload;
}
