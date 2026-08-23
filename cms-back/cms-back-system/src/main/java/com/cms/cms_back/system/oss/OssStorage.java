package com.cms.cms_back.system.oss;

import java.util.List;

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

    /**
     * 确认对象已上传
     * @param ObjectKey
     * @return
     */
    OssObjectMeta head(String ObjectKey);

    /**
     * 完成分片上传
     * @param ObjectKey
     * @param uploadId
     * @param parts
     * @return
     */
    String completeMultipart(String ObjectKey, String uploadId, List<OssUploadPart> parts);

}
