package com.cms.cms_back.system.mapper;

import java.util.Collection;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.cms_back.pojo.entity.ArticleMediaRefs;

@Mapper
public interface ArticleMediaRefMapper extends BaseMapper<ArticleMediaRefs> {

    /** 查询某篇文章引用的所有file_id */
    List<Long> selectFileIdsByArticleId(@Param("articleId") Long articleId);

    /** 查询fileIds中被已发布文章引用的file_id */
    List<Long> selectPublishedFileIds(@Param("fileIds") Collection<Long> fileIds);
}
