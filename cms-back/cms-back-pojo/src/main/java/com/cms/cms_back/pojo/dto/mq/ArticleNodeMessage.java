package com.cms.cms_back.pojo.dto.mq;

import java.io.Serializable;

import lombok.Data;

/**
 * 文章节点消息体
 * ArticleNodeMessage
 */
@Data
public class ArticleNodeMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 节点ID
     */
    private Long nodeId;

    /**
     * 节点标题
     */
    private String title;
}
