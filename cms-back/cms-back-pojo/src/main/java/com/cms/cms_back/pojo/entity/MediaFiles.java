package com.cms.cms_back.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cms.cms_back.pojo.enums.MediaFilesAccessLevelType;
import com.cms.cms_back.pojo.enums.MediaFilesBizType;
import com.cms.cms_back.pojo.enums.MediaFilesStatus;

import java.time.LocalDateTime;

import lombok.Data;

@TableName("media_files")
@Data
public class MediaFiles {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** OSS桶内路径 */
    private String objectKey;

    /** 原始文件名 */
    private String originalName;

    /** 文件类型 */
    private String contentType;

    /** 文件大小(字节) */
    private Long sizeBytes;

    /** 业务类型 */
    private MediaFilesBizType bizType;

    /** 空间ID */
    private Long spaceId;

    /** 访问权限 */
    private MediaFilesAccessLevelType accessLevel;

    /** 状态 */
    private MediaFilesStatus status;

    /** OSS 分片上传ID */
    private String uploadId;

    /** OSS文件ETag */
    private String etag;

    /** 创建者ID */
    private Long createdBy;

    /** 创建时间 */ 
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

}
