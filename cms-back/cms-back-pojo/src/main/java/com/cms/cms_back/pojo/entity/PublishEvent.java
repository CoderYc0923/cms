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

    /** 文章ID */
    private Long articleId;

    /** 空间ID */
    private Long spaceId;

    /** 事件类型 */
    private PublishEventType eventType;

    /** 发生时间 */
    private LocalDateTime occurredAt;

    /** JSON 摘要，先按字符串存 */
    private String payload;
}
