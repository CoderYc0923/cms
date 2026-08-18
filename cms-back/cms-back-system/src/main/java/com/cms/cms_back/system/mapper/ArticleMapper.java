package com.cms.cms_back.system.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.cms_back.pojo.entity.Article;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
    
}
