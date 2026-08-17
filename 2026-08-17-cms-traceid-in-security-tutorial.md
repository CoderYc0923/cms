# TraceIdFilter 挂入 Spring Security 教程

**日期：** 2026-08-17  
**决策：** TraceId 与 JWT 都放在 **SecurityFilterChain** 里统一管理（顺序可控、风格一致）  
**适用：** 当前 `cms-back-framework`（已有 `TraceIdFilter`、`CmsSecurityConfig`）

---

## 1. 为什么改成挂 Security

| 点 | 说明 |
|----|------|
| 统一 | 请求链路相关 Filter 都在一条 `SecurityFilterChain` 里看 |
| 顺序明确 | 固定：`TraceId → JWT → …`，不靠 `@Order` 和 Security 抢优先级 |
| 避免双重执行 | 去掉 `@Component` 自动注册，只 `addFilterBefore` 一次 |

代价：只对走这条 Security 链的请求生效。你们单体一条默认链覆盖 `/api/**`、actuator 等，一般够用。

---

## 2. 目标顺序

```text
请求进入 SecurityFilterChain
  → TraceIdFilter          // 写 MDC.traceId、响应头 X-Trace-Id
  → JwtAuthenticationFilter // 解析 Bearer，设 SecurityContext
  → … Security 其余过滤器 …
  → Controller
```

原则：**先打点，再鉴权**；鉴权失败打的 401 日志也能带上 `traceId`（前提 logback pattern / JSON 含 `%X{traceId}`）。

---

## 3. 改法（两步，缺一不可）

### 步骤 A：`TraceIdFilter` 去掉 Servlet 自动注册

**不要**再用 `@Component` + `@Order`（那会进容器全局 Filter，和 Security 再 add 会跑两次）。

改为普通类，由 Security 配置里 `@Bean` 创建：

**路径：** `cms-back-framework/.../web/TraceIdFilter.java`

```java
package com.cms.cms_back.framework.web;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 请求追踪：写入 MDC，并回写响应头 X-Trace-Id。
 * 不使用 @Component，由 CmsSecurityConfig 注册进 SecurityFilterChain。
 */
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_HEADER = "X-Trace-Id";
    private static final String TRACE_ID_KEY = "traceId";
    private static final String PATH_KEY = "path";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(TRACE_ID_KEY, traceId);
        MDC.put(PATH_KEY, request.getRequestURI());
        response.setHeader(TRACE_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.clear();
        }
    }
}
```

要点：

- **删除** `@Component`、`@Order`  
- 类可以继续放在 `framework.web` 包（观测组件）；注册动作在 security 配置里  

---

### 步骤 B：在 `CmsSecurityConfig` 里注册并排好序

```java
package com.cms.cms_back.framework.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
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

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CmsSecurityProperties.class)
public class CmsSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RestAuthEntryPoint authEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

    public CmsSecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthEntryPoint authEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.authEntryPoint = authEntryPoint;
        this.accessDeniedHandler = accessDeniedHandler;
    }

    /** TraceId 仅作为 Security 链上的 Filter Bean，不额外 @Component */
    @Bean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            TraceIdFilter traceIdFilter) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(
                                "/api/admin/auth/login",
                                "/api/admin/auth/refresh",
                                "/api/admin/auth/logout").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/admin/**").authenticated()
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .cors(Customizer.withDefaults());

        // 顺序：先 TraceId，再 JWT（都插在 UsernamePasswordAuthenticationFilter 之前）
        // 注意：后 add 的在更前面 —— 先 add JWT，再 add TraceId，最终 TraceId 在最前
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(traceIdFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 顺序怎么记

```text
addFilterBefore(A, B)  →  A 在 B 前面
```

上面两行之后：

```text
TraceIdFilter → JwtAuthenticationFilter → UsernamePasswordAuthenticationFilter → …
```

也可以写成（等价思路）：

```java
http.addFilterBefore(traceIdFilter, UsernamePasswordAuthenticationFilter.class);
http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
// 若两次都相对同一个锚点，后注册的 jwt 可能跑到 trace 前面，容易搞反
// 因此推荐：jwt 相对 UsernamePassword…；trace 相对 JwtAuthenticationFilter（如上一示例）
```

---

## 4. JwtAuthenticationFilter 还要不要 `@Component`

两种都行：

| 写法 | 说明 |
|------|------|
| `@Component` + 构造注入进 Config | 简单，和现在教程一致 |
| 也改成 `@Bean` 方法创建 | 和 TraceId 完全同一风格 |

TraceId **必须**去掉 `@Component`；JWT 可以暂时保留 `@Component`。

---

## 5. 日志是否带上 traceId

Filter 只负责 `MDC.put`。local 的 `logback-spring.xml` 建议：

```xml
<pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} [traceId=%X{traceId}] - %msg%n</pattern>
```

prod 的 LogstashEncoder 继续 `includeMdcKeyName: traceId`。

---

## 6. 自测

```powershell
curl -i http://127.0.0.1:8080/actuator/health
# 响应头应有：X-Trace-Id: ...

curl -i http://127.0.0.1:8080/actuator/health -H "X-Trace-Id: my-fixed-id"
# 响应头应回显：X-Trace-Id: my-fixed-id
```

调一个会 401 的 admin 接口，日志里应能看到同一个 `traceId`（pattern 配好的前提下）。

---

## 7. 常见坑

| 现象 | 原因 | 处理 |
|------|------|------|
| trace 执行两次 / 头重复 | 仍保留 `@Component` 又 `addFilterBefore` | 去掉 `@Component` |
| 有 JWT 日志却无 traceId | TraceId 加在 JWT **后面**了 | `addFilterBefore(traceId, JwtAuthenticationFilter.class)` |
| 编译找不到 `JwtAuthenticationFilter` | 类尚未创建 | 先按鉴权教程落地 JWT Filter，或暂时只挂 TraceId |

---

## 8. Checklist

1. [ ] `TraceIdFilter` 去掉 `@Component` / `@Order`  
2. [ ] `CmsSecurityConfig` 增加 `@Bean TraceIdFilter`  
3. [ ] `addFilterBefore(traceIdFilter, JwtAuthenticationFilter.class)`  
4. [ ] `addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)`  
5. [ ] logback local pattern 带 `%X{traceId}`  
6. [ ] curl 检查响应头 `X-Trace-Id`  

---

## 9. 和主鉴权教程的关系

主文档：`2026-08-17-cms-auth-spring-security-jwt-tutorial.md`  

其中若仍写「TraceId 用 @Component、Security 不要 add」，以 **本文为准**：统一挂 Security，顺序 TraceId → JWT。

---

**一句话：** 去掉 TraceId 的 `@Component`，在 `SecurityFilterChain` 里 `addFilterBefore`，放在 JWT 前面，即统一又安全。
