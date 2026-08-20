package com.cms.cms_back.pojo.constants.mq.consumer;

/**
 * 文章节点消息常量
 * ArticleNodeConstant
 */
public final class ArticleNodeConstant {

    public static final String TOPIC = "article-node";

    public static final String GROUP = "article-node-consumer";

    /** 创建文章节点消息标签 */
    public static final String CREATE_TAG = "create";

    public ArticleNodeConstant() {}
}
