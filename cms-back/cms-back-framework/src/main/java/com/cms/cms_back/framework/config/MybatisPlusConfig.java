package com.cms.cms_back.framework.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MybatisPlus配置类
 * @author Cyrus
 * @date 2026-08-17
 * MybatisPlusConfig
 */
@Configuration
@MapperScan("com.cms.cms_back.system.mapper")
public class MybatisPlusConfig {

}
