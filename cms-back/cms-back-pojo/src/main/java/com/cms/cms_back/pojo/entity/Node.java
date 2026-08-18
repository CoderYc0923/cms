package com.cms.cms_back.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cms.cms_back.pojo.enums.NodeStatus;
import com.cms.cms_back.pojo.enums.NodeType;

import lombok.Data;

@TableName("nodes")
@Data
public class Node {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long spaceId;

    private Long parentId;

    private NodeType type;

    private String title;

    private Integer sort;

    private NodeStatus status;

    private LocalDateTime deletedAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
