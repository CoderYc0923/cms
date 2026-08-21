package com.cms.cms_back.pojo.vo.upload;

import com.cms.cms_back.pojo.enums.UploadModeType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InitUploadVO {

    private Long fileId;

    /** 模式: single-单文件, multipart-分片上传 */
    private UploadModeType mode;

    /** 小文件 */
    /** 上传URL */
    private String putUrl;

    /** 上传头部 */
    private InitSingleHeaderVO headers;

    /** 大文件 */
    /** 分片上传ID */
    private String uploadId;

    /** 分片大小 */
    private Long partSize;

    /** 分片数量 */
    private Integer partCount;
}
