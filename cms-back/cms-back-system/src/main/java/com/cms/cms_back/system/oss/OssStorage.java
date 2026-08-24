package com.cms.cms_back.system.oss;

import java.util.List;

public interface OssStorage {

    /**
     * 预签名获取
     * @param objectKey
     * @param expireSeconds
     * @return
     */
    String presignGet(String objectKey, long expireSeconds);

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

    /**
     * 取消分片上传
     * @param objectKey
     * @param uploadId
     */
    void abortMultipart(String objectKey, String uploadId);

    /**
     * 删除对象（单文件上传abort时清理已经put的对象）
     * @param objectKey
     */
    void deleteObject(String objectKey);

}
