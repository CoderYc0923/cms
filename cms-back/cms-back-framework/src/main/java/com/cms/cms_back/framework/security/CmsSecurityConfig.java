package com.cms.cms_back.framework.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

import com.cms.cms_back.framework.web.TraceIdFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CmsSecurityProperties.class)
public class CmsSecurityConfig {

    @Bean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, TraceIdFilter traceIdFilter) throws Exception {
        http.csrf(csrf -> csrf.disable());

        return http.build();
    }
}
