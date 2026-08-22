package com.cms.cms_back.system.oss;

import java.util.Date;

import org.springframework.stereotype.Component;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ObjectMetadata;

@Component
public class AliyunOssStorage implements OssStorage {

    private final OssProperties ossProperties;
    private final OSSClient ossClient;

    public AliyunOssStorage(OssProperties ossProperties, OSSClient ossClient) {
        this.ossClient = ossClient;
        this.ossProperties = ossProperties;
    }

    /**
     * 生成上传URL
     * @param ObjectKey
     * @param contentType
     * @param expireSeconds
     * @return
     */
    @Override
    public String presignPut(String ObjectKey, String contentType, long expireSeconds) {
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);

        /** 生成预签名请求 */
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(ossProperties.getBucket(), ObjectKey, HttpMethod.PUT);
        request.setExpiration(expiration);
        request.setContentType(contentType);

        /** 生成预签名URL */
        return ossClient.generatePresignedUrl(request).toString();
    }

    /**
     * 初始化分片上传
     * @param ObjectKey
     * @param contentType
     * @return
     */
    @Override
    public String initialMultipart(String ObjectKey, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);

        /** 初始化分片上传请求 */
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(ossProperties.getBucket(), ObjectKey, metadata);
        InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
        
        return result.getUploadId();
    }
}
