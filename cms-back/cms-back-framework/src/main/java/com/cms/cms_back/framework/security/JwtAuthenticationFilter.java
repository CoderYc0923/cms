package com.cms.cms_back.framework.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jwt.JWTClaimsSet;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT认证过滤器，用于验证JWT令牌并设置认证上下文
 * JwtAuthenticationFilter
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String ACCESS_TOKEN_COOKIE = "CMS_ACCESS_TOKEN";
    private static final String LEGACY_TOKEN_COOKIE = "BACK_USERID";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                /* 解析并验证JWT,获取用户信息声明 */
                JWTClaimsSet claims = jwtService.parseAndValidate(token);
                /* 获取用户ID和用户名 */
                Long userId = Long.valueOf(claims.getSubject());
                String username = (String) claims.getClaim("username");

                /*
                 * 参数说明：
                 * 1. principal: 认证主体，放入当前用户信息体
                 * 2. credentials: 凭证，通常为null，因为JWT不包含密码
                 * 3. authorities: 权限列表，这里使用硬编码的ROLE_ADMIN角色
                 */
                var auth = new UsernamePasswordAuthenticationToken(new UserInfo(userId, username), null, List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN")));

                /* 设置认证详情，包括用户名 */
                auth.setDetails(username);
                /* 设置认证上下文，将认证信息保存到SecurityContext中 */
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JOSEException | java.text.ParseException | RuntimeException e) {
                SecurityContextHolder.clearContext();
                /* 如果JWT解析或验证失败，清除认证上下文 */
            }
        }
        chain.doFilter(request, response);
    }

    /**
     * 优先 Authorization Bearer；其次 Cookie（供 img 等同源资源请求鉴权）
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            String name = cookie.getName();
            if (ACCESS_TOKEN_COOKIE.equals(name) || LEGACY_TOKEN_COOKIE.equals(name)) {
                String value = cookie.getValue();
                if (value != null && !value.isBlank()) {
                    return value;
                }
            }
        }
        return null;
    }
}
