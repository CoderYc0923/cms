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

    /** 空间ID */
    private Long spaceId;

    /** 父节点ID */
    private Long parentId;

    /** 节点类型 */
    private NodeType type;

    /** 标题 */
    private String title;

    /** 排序 */
    private Integer sort;

    /** 
     * 节点状态
     * @see NodeStatus
     */
    private NodeStatus status;

    /** 删除时间 */
    private LocalDateTime deletedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}
