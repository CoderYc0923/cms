package com.cms.cms_back.pojo.vo.upload;

import lombok.Data;

/**
 * 签发分片VO
 * SignPartVO
 */
@Data
public class SignPartVO {

    /** 分片号 */
    private Integer partNumber;

    /** 上传URL */
    private String putUrl;
}
