package com.cms.cms_back.system.oss;

/**
 * OSS对象元数据
 * OssObjectMeta
 * @param etag
 * @param sizeBytes
 */
public record OssObjectMeta(String etag, long sizeBytes) {

}
