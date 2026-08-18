package com.cms.cms_back.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cms.cms_back.pojo.enums.PublishStatus;

import lombok.Data;

@TableName("articles")
@Data
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 节点ID */
    private Long nodeId;

    /** 空间ID */
    private Long spaceId;

    /** 内容 */
    private String content;

    /** 
     * 发布状态
     * @see PublishStatus
     */
    private PublishStatus publishStatus;

    /** 发布时间 */
    private LocalDateTime publishAt;

    /** 创建者ID */
    private Long createdBy;

    /** 更新者ID */
    private Long updatedBy;

    /** 删除时间 */
    private LocalDateTime deletedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
