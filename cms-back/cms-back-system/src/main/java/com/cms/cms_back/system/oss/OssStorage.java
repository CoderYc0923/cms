package com.cms.cms_back.system.oss;

import java.util.List;

public interface OssStorage {

    /**
     * 预签名上传
     * 
     * @param objectKey
     * @param contentType
     * @param expireSeconds
     * @return
     */
    String presignPut(String objectKey, String contentType, long expireSeconds);

    /**
     * 分片预签名上传
     * @param objectKey
     * @param uploadId
     * @param partNumber
     * @param expireSeconds
     * @return
     */
    String presignUploadPart(String objectKey, String uploadId, int partNumber, long expireSeconds);

    /**
     * 初始化分片上传
     * 
     * @param objectKey
     * @param contentType
     * @return
     */
    String initialMultipart(String objectKey, String contentType);

    /**
     * 确认对象已上传
     * 
     * @param objectKey
     * @return
     */
    OssObjectMeta head(String objectKey);

    /**
     * 完成分片上传
     * 
     * @param objectKey
     * @param uploadId
     * @param parts
     * @return
     */
    String completeMultipart(String objectKey, String uploadId, List<OssUploadPart> parts);

}
