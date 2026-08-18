package com.cms.cms_back.framework.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    private static final List<String> ALLOWED_ORIGINS = List.of(
        "http://localhost:*",
        "http://127.0.0.1:*"
    );

    private static final List<String> ALLOWED_METHODS = List.of(
        "GET",
        "POST",
        "PUT",
        "DELETE",
        "OPTIONS"
    );

    private static final List<String> ALLOWED_HEADERS = List.of(
        "*"
    );

    private static final List<String> EXPOSED_HEADERS = List.of(
        "X-Trace-Id",
        "Authorization"
    );

    /**
     * 配置CORS
     * @return
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(ALLOWED_ORIGINS);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setExposedHeaders(EXPOSED_HEADERS);
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
