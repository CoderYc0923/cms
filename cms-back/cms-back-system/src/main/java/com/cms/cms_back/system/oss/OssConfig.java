package com.cms.cms_back.system.oss;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;

@Configuration
@EnableConfigurationProperties(OssProperties.class)
public class OssConfig {

    /**
     * 创建OSS客户端
     * 销毁时关闭客户端
     * @param ossProperties
     * @return
     */
    @Bean(destroyMethod = "shutdown")
    public OSS ossClient(OssProperties ossProperties) {
        String endpoint = normalizeEndpoint(ossProperties.getEndpoint());
        String accessKeyId = requireConfig(ossProperties.getAccessKeyId(), "OSS_ACCESS_KEY_ID");
        String accessKeySecret = requireConfig(ossProperties.getAccessKeySecret(), "OSS_ACCESS_KEY_SECRET");

        return new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
    }

    private String normalizeEndpoint(String endpoint) {
        String value = requireConfig(endpoint, "OSS_ENDPOINT");
        String trimmed = value.trim();
        if (trimmed.startsWith("https://")) {
            return trimmed.substring("https://".length());
        }
        if (trimmed.startsWith("http://")) {
            return trimmed.substring("http://".length());
        }
        return trimmed;
    }

    private String requireConfig(String value, String envKey) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("OSS 配置缺失: " + envKey + "，请检查 cms-back/.env");
        }
        if (value.contains("${")) {
            throw new IllegalStateException(
                    "OSS 配置未解析: " + envKey + "，请确认 cms-back/.env 存在且 application.yml 已 import");
        }
        return value;
    }
}
