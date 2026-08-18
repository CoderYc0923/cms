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

    /** 用户名 */
    private String username;

    /** 密码 */
    private String password;

    /** 显示名称 */
    private String displayName;

    /** 
     * 用户状态
     * @see UserStatus
     */
    private UserStatus status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
