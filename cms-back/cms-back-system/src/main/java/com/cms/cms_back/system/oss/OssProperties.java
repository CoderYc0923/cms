package com.cms.cms_back.system.oss;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    /**
     * 阿里云OSS的Endpoint
     */
    private String endpoint;

    /**
     * 阿里云OSS的Bucket
     */
    private String bucket;

    /**
     * 阿里云OSS的Key前缀
     */
    private String ossKeyPrefix = "cms";

    /**
     * 阿里云OSS的Access Key ID
     */
    private String accessKeyId;

    /**
     * 阿里云OSS的Access Key Secret
     */
    private String accessKeySecret;

    /**
     * 签名获取的过期时间
     * 10分钟
     */
    private long signedGetExpireSeconds = 600;

    /**
     * 签名上传的过期时间
     * 1小时
     */
    private long signedPutExpireSeconds = 3600;

    /**
     * 分片上传的阈值
     * 10MB
     */
    private long multipartThresholdBytes = 10485760;

    /**
     * 分片上传的每个分片的大小
     * 8MB
     */
    private long multipartPartSizeBytes = 8388608;

    /**
     * 最大图片大小
     * 10MB
     */
    private long maxImageBytes = 10485760;

    /**
     * 最大视频大小
     * 500MB
     */

    private long maxVideoBytes = 524288000;

    /**
     * 允许的图片类型
     */
    private List<String> allowedImageTypes = new ArrayList<>();

    /**
     * 允许的视频类型
     */
    private List<String> allowedVideoTypes = new ArrayList<>();

}
