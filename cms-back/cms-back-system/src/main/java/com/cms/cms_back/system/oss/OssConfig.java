package com.cms.cms_back.system.oss;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return new OSSClientBuilder().build(
            ossProperties.getEndpoint(),
            ossProperties.getAccessKeyId(),
            ossProperties.getAccessKeySecret()
        );
    }
}
