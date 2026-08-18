package com.cms.cms_back.framework.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.cms.cms_back.framework.web.TraceIdFilter;

import jakarta.servlet.Filter;

/**
 * 安全配置类
 * 配置Spring Security的过滤器链、认证方式、权限控制等
 * CmsSecurityConfig
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CmsSecurityProperties.class)
public class CmsSecurityConfig {

    private static final String[] PUBLIC_URLS = {
            "/api/admin/auth/login",
            "/api/admin/auth/refresh",
            "/api/admin/auth/logout",
            "/api/public/**",
            "/actuator/**"
    };

    private final RestAuthEntryPoint authEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TraceIdFilter traceIdFilter;

    public CmsSecurityConfig(RestAuthEntryPoint authEntryPoint, RestAccessDeniedHandler accessDeniedHandler,
            JwtAuthenticationFilter jwtAuthenticationFilter, TraceIdFilter traceIdFilter) {
        this.authEntryPoint = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.traceIdFilter = traceIdFilter;
    }

    /**
     * 关掉traceIdFilter的Servlet自动注册，防止重复注册
     * 
     * @param filter
     * @return
     */
    @Bean
    public FilterRegistrationBean<Filter> disableTraceIdFilterRegistration(TraceIdFilter filter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 关掉jwtAuthenticationFilter的Servlet自动注册，防止重复注册
     * 
     * @param filter
     * @return
     */
    @Bean
    public FilterRegistrationBean<Filter> disableJwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    /**
     * 添加安全过滤器链
     * 
     * @param http
     * @param traceIdFilter
     * @return
     * @throws Exception
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // 禁用CSRF保护
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))// 禁用会话管理
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll() // 公开URL
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // 允许OPTIONS请求
                        .requestMatchers("/api/admin/**").authenticated() // 需要认证的URL
                        .anyRequest().permitAll() // 其他URL默认允许访问
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint) // 认证失败处理
                        .accessDeniedHandler(accessDeniedHandler) // 访问拒绝处理
                )
                .cors(Customizer.withDefaults()); // 使用默认的CORS配置，找找名为 / 类型为 CorsConfigurationSource 的 Bean读取配置

        /* 添加过滤器，先执行traceIdFilter，再执行jwtAuthenticationFilter */
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(traceIdFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 添加密码编码器
     * 
     * @return
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
