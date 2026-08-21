package com.cms.cms_back.pojo.vo.upload;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 完成上传VO
 * CompleteUploadVO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CompleteUploadVO {

    /** 稳定URL */
    private String stableUrl;
}
