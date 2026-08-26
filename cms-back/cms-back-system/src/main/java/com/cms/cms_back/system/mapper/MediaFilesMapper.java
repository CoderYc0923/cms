package com.cms.cms_back.system.mapper;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.cms_back.pojo.entity.MediaFiles;

@Mapper
public interface MediaFilesMapper extends BaseMapper<MediaFiles> {

    /** 
     * 查询上传超过指定时间未完成的文件
     * @param deadline 截止时间
     * @param limit 限制条数
     * @return 文件列表
     */
    List<MediaFiles> selectStaleUploading(@Param("deadline") LocalDateTime deadline, @Param("limit") int limit);

    /**
     * 查询ready文件无引用超过指定天数将被删除的文件
     * @param deadline 截止时间
     * @param limit 限制条数
     * @return 文件列表
     */
    List<MediaFiles> selectOrphanReadyPrivate(@Param("deadline") LocalDateTime deadline, @Param("limit") int limit);
}
