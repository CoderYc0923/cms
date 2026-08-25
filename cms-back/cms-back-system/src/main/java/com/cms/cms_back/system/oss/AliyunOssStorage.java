package com.cms.cms_back.system.oss;

import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PartETag;

@Component
public class AliyunOssStorage implements OssStorage {

    private final OssProperties ossProperties;
    private final OSS ossClient;

    public AliyunOssStorage(OssProperties ossProperties, OSS ossClient) {
        this.ossClient = ossClient;
        this.ossProperties = ossProperties;
    }

    /**
     * 预签名获取
     */
    @Override
    public String presignGet(String objectKey, long expireSeconds) {
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(ossProperties.getBucket(), objectKey, HttpMethod.GET);
        request.setExpiration(expiration);

        return ossClient.generatePresignedUrl(request).toString();
    }

    /**
     * 生成上传URL
     * @param objectKey
     * @param contentType
     * @param expireSeconds
     * @return
     */
    @Override
    public String presignPut(String objectKey, String contentType, long expireSeconds) {
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);

        /** 生成预签名请求 */
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(ossProperties.getBucket(), objectKey, HttpMethod.PUT);
        request.setExpiration(expiration);
        request.setContentType(contentType);

        /** 生成预签名URL */
        return ossClient.generatePresignedUrl(request).toString();
    }

    /**
     * 生成分片预签名上传URL
     * @param objectKey
     * @param uploadId
     * @param partNumber
     * @param expireSeconds
     * @return
     */
    @Override
    public String presignUploadPart(String objectKey, String uploadId, int partNumber, long expireSeconds) {
        Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000L);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(ossProperties.getBucket(), objectKey, HttpMethod.PUT);

        request.setExpiration(expiration);
        /* 添加分片上传ID和分片序号query参数 */
        request.addQueryParameter("uploadId", uploadId);
        request.addQueryParameter("partNumber", String.valueOf(partNumber));

        return ossClient.generatePresignedUrl(request).toString();
    }

    /**
     * 初始化分片上传
     * @param objectKey
     * @param contentType
     * @return
     */
    @Override
    public String initialMultipart(String objectKey, String contentType) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType(contentType);

        /** 初始化分片上传请求 */
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(ossProperties.getBucket(), objectKey, metadata);
        InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
        
        return result.getUploadId();
    }

    /**
     * 确认对象已上传
     */
    @Override
    public OssObjectMeta head(String objectKey) {
        /** 获取对象元数据 */
        ObjectMetadata metadata = ossClient.getObjectMetadata(ossProperties.getBucket(), objectKey);
        return new OssObjectMeta(metadata.getETag(), metadata.getContentLength());
    }

    /**
     * 完成分片上传
     */
    @Override
    public String completeMultipart(String objectKey, String uploadId, List<OssUploadPart> parts) {
        List<PartETag> partETags = parts.stream()
            .sorted(Comparator.comparingInt(OssUploadPart::partNumber))
            .map(p -> new PartETag(p.partNumber(), normalizeEtag(p.etag())))
            .collect(Collectors.toList());

        CompleteMultipartUploadRequest request = new CompleteMultipartUploadRequest(ossProperties.getBucket(), objectKey, uploadId,partETags);
        CompleteMultipartUploadResult result = ossClient.completeMultipartUpload(request);
        return result.getETag();
    }

    /**
     * 取消分片上传
     */
    @Override
    public void abortMultipart(String objectKey, String uploadId) {
        AbortMultipartUploadRequest request = new AbortMultipartUploadRequest(ossProperties.getBucket(), objectKey, uploadId);
        ossClient.abortMultipartUpload(request);  
    }

    /**
     * 删除对象
     */
    @Override
    public void deleteObject(String objectKey) {
       ossClient.deleteObject(ossProperties.getBucket(), objectKey);
    }

    /**
     * 标准化ETag，确保ETag以双引号包裹
     * @param etag
     * @return
     */
    private String normalizeEtag(String etag) {
        if (!StringUtils.hasText(etag)) {
            return null;
        }
        
        String trimmed = etag.trim();
        if (trimmed.startsWith("\"")) {
            return trimmed;
        }
        return "\"" + trimmed + "\"";
    }
}
