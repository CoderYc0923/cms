package com.cms.cms_back.system.oss;

public interface OssStorage {

    /**
     * 预签名上传
     * @param ObjectKey
     * @param contentType
     * @param expireSeconds
     * @return
     */
    String presignPut(String ObjectKey, String contentType, long expireSeconds);

    /**
     * 初始化分片上传
     * @param ObjectKey
     * @param contentType
     * @return
     */
    String initialMultipart(String ObjectKey, String contentType);

}
