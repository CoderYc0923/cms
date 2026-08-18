package com.cms.cms_back.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cms.cms_back.pojo.enums.SpaceStatus;

import java.time.LocalDateTime;

import lombok.Data;

@TableName("spaces")
@Data
public class Space {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String slug;

    private String description;

    private Integer sort;

    private SpaceStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
