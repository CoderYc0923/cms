package com.cms.cms_back.pojo.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cms.cms_back.pojo.enums.UserStatus;

import lombok.Data;

@TableName("users")
@Data
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private String displayName;

    private UserStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
