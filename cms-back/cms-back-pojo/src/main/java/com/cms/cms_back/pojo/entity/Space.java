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

    /** 名称 */
    private String name;

    /** 别名 */
    private String slug;

    /** 描述 */
    private String description;

    /** 排序 */
    private Integer sort;

    /** 
     * 空间状态
     * @see SpaceStatus
     */
    private SpaceStatus status;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
