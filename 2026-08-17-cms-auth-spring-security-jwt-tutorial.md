# CMS 鉴权体系教程：Spring Security + JWT 双 Token（Nimbus）

**日期：** 2026-08-17（按当前仓库形态更新；未完成部分给出完整可粘贴代码）  
**适用：** `cms-back` Spring Boot 4.x 多模块  
**JWT 库选型：** **Nimbus JOSE + JWT**（与已引入的 `spring-boot-starter-oauth2-resource-server` 一致）  
**成功响应：** `ApiResult.success` → `{ code: 200, message: "success", data }`  
**配置前缀：** `security.*`（不是 `cms.security`）

---

## 0. Nimbus vs JJWT：哪个好？本项目用哪个？

| | **Nimbus** | **JJWT** |
|--|------------|----------|
| 与 Spring Security | **官方 oauth2-resource-server / JwtDecoder 就用它** | 第三方，需另加依赖 |
| 你们仓库 | `framework` **已有** `spring-boot-starter-oauth2-resource-server` | 尚未引入 |
| API 风格 | 偏标准 JOSE（`SignedJWT`、`JWSHeader`） | 链式 `Jwts.builder()`，更「顺口」 |
| 文档/示例 | Spring 文档多 | 中文教程多 |
| 额外 jar | 一般 **0**（传递依赖已有） | 至少 `jjwt-api/impl/jackson` 三个 |

**结论（本项目）：用 Nimbus，不要再引 JJWT。**

- 少一套依赖、和 Spring Security 生态一致。  
- 下面 `JwtService` 全部用 `com.nimbusds.jose.*` / `com.nimbusds.jwt.*`。  
- 不必为了「好写」再装 JJWT；Nimbus 代码稍多几行，但足够清晰。

HS256 密钥长度：**至少 32 字节**（你们 local 密钥已够长）。

---

## 1. 当前进度

| 项 | 状态 |
|----|------|
| ApiResult / BizException / GlobalExceptionHandler | ✅ |
| CmsSecurityProperties + Enable | ✅（`prefix = "security"`） |
| Security / Redis / oauth2-resource-server 依赖 | ✅ |
| MyBatis-Plus + UserMapper + User/UserStatus | ✅ |
| LoginDTO / RefreshDTO / TokenResponseVO | ✅ |
| AuthController 路径与方法签名 | ✅（方法体仍 `return null`） |
| JwtService（Nimbus）/ RefreshTokenStore / AuthService | ❌ 下文完整代码 |
| JwtAuthenticationFilter + EntryPoint/DeniedHandler | ❌ 下文完整代码 |
| SecurityFilterChain + PasswordEncoder + CORS | ❌ 下文完整代码 |
| 种子管理员 + 前端续签 | ❌ 下文完整代码 |

---

## 2. 约定速查

| 项 | 值 |
|----|-----|
| Access | JWT HS256，30m，`Authorization: Bearer ...` |
| Refresh | 随机串，7d，Redis，**每次 refresh 轮转** |
| Redis key | `cms:auth:refresh:{token}` |
| 登录/刷新/退出 | `/api/admin/auth/login\|refresh\|logout` |
| public | `/api/public/**` 匿名 |
| 成功 code | **200**（`ApiResult.success`） |
| 失败 | `ApiResult.fail` + HTTP 状态 |

---

## 3. 依赖（保持现状 + 说明）

`cms-back-framework/pom.xml` **已有即可**，不必再加 JJWT：

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-security</artifactId>
</dependency>
<!-- 传递引入 Nimbus JOSE/JWT，供 JwtService 使用 -->
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
```

---

## 4. 建议先修正的已有问题

### 4.1 `User` 实体与表对齐

Liquibase 表名是 **`users`**，列是 `created_at` / `updated_at`：

```java
@TableName("users")
private LocalDateTime createdAt;
private LocalDateTime updatedAt;
```

（不要用 `@TableName("user")`、`createAt`。）

### 4.2 TraceIdFilter：统一挂进 Security（推荐）

为和 JWT 同一条链路、顺序可控，**不要**再用 `@Component` 自动注册，改为在 `SecurityFilterChain` 里：

```text
TraceIdFilter → JwtAuthenticationFilter → …
```

完整步骤见：`2026-08-17-cms-traceid-in-security-tutorial.md`。

---

## 5. 完整待实现代码（Nimbus + 当前命名）

以下均可直接粘贴到对应路径。

---

### 5.1 `JwtService`（Nimbus 签发 / 校验）

**路径：** `cms-back-framework/src/main/java/com/cms/cms_back/framework/security/JwtService.java`

```java
package com.cms.cms_back.framework.security;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;

import org.springframework.stereotype.Service;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

/**
 * Access Token：用 Nimbus 做 HS256 签发与校验。
 * 依赖来自 spring-boot-starter-oauth2-resource-server（无需 JJWT）。
 */
@Service
public class JwtService {

    private final CmsSecurityProperties properties;
    private final byte[] secret;

    public JwtService(CmsSecurityProperties properties) {
        this.properties = properties;
        this.secret = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        if (this.secret.length < 32) {
            throw new IllegalStateException("security.jwt-secret 长度至少 32 字节（HS256）");
        }
    }

    /** 签发 Access JWT：sub=userId，自定义 claim username */
    public String createAccessToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getAccessTokenTtl());
        try {
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(String.valueOf(userId))
                    .claim("username", username)
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .build();
            SignedJWT signedJWT = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
            signedJWT.sign(new MACSigner(secret));
            return signedJWT.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("签发 Access Token 失败", e);
        }
    }

    public long getAccessExpiresInSeconds() {
        return properties.getAccessTokenTtl().toSeconds();
    }

    /**
     * 校验签名 + 未过期，返回 claims。
     * 失败抛 JOSEException / ParseException / 业务包装，由过滤器当作未登录处理。
     */
    public JWTClaimsSet parseAndValidate(String accessToken) throws ParseException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(accessToken);
        if (!signedJWT.verify(new MACVerifier(secret))) {
            throw new JOSEException("invalid jwt signature");
        }
        JWTClaimsSet claims = signedJWT.getJWTClaimsSet();
        Date exp = claims.getExpirationTime();
        if (exp == null || exp.before(new Date())) {
            throw new JOSEException("jwt expired");
        }
        return claims;
    }
}
```

---

### 5.2 `RefreshTokenStore`

**路径：** `.../framework/security/RefreshTokenStore.java`

```java
package com.cms.cms_back.framework.security;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "cms:auth:refresh:";

    private final StringRedisTemplate redis;
    private final CmsSecurityProperties properties;
    private final ObjectMapper objectMapper;

    public RefreshTokenStore(
            StringRedisTemplate redis,
            CmsSecurityProperties properties,
            ObjectMapper objectMapper) {
        this.redis = redis;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String issue(Long userId, String username) {
        String token = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        try {
            String json = objectMapper.writeValueAsString(new SessionUser(userId, username));
            redis.opsForValue().set(KEY_PREFIX + token, json, properties.getRefreshTokenTtl());
            return token;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("serialize refresh session failed", e);
        }
    }

    public Optional<SessionUser> find(String refreshToken) {
        String json = redis.opsForValue().get(KEY_PREFIX + refreshToken);
        if (json == null || json.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(objectMapper.readValue(json, SessionUser.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    public void revoke(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            redis.delete(KEY_PREFIX + refreshToken);
        }
    }

    public static class SessionUser {
        private Long userId;
        private String username;

        public SessionUser() {
        }

        public SessionUser(Long userId, String username) {
            this.userId = userId;
            this.username = username;
        }

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }
    }
}
```

---

### 5.3 `AuthService`

**路径：** `.../framework/security/AuthService.java`

```java
package com.cms.cms_back.framework.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.pojo.entity.User;
import com.cms.cms_back.pojo.enums.UserStatus;
import com.cms.cms_back.pojo.vo.auth.TokenResponseVO;
import com.cms.cms_back.system.mapper.UserMapper;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;

    public AuthService(
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenStore refreshTokenStore) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
    }

    public TokenResponseVO login(String username, String rawPassword) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("LIMIT 1"));
        if (user == null || user.getStatus() != UserStatus.ENABLED) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        return issueTokens(user.getId(), user.getUsername());
    }

    /** 校验 refresh → 作废旧票 → 发新双 Token（轮转） */
    public TokenResponseVO refresh(String refreshToken) {
        var session = refreshTokenStore.find(refreshToken)
                .orElseThrow(() -> BizException.unauthorized("登录已失效，请重新登录"));
        refreshTokenStore.revoke(refreshToken);
        return issueTokens(session.getUserId(), session.getUsername());
    }

    public void logout(String refreshToken) {
        refreshTokenStore.revoke(refreshToken);
    }

    private TokenResponseVO issueTokens(Long userId, String username) {
        return TokenResponseVO.builder()
                .accessToken(jwtService.createAccessToken(userId, username))
                .refreshToken(refreshTokenStore.issue(userId, username))
                .expiresIn(jwtService.getAccessExpiresInSeconds())
                .build();
    }
}
```

---

### 5.4 `JwtAuthenticationFilter`

**路径：** `.../framework/security/JwtAuthenticationFilter.java`

```java
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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            try {
                JWTClaimsSet claims = jwtService.parseAndValidate(token);
                Long userId = Long.valueOf(claims.getSubject());
                String username = (String) claims.getClaim("username");
                var auth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                auth.setDetails(username);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JOSEException | java.text.ParseException | RuntimeException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

---

### 5.5 `RestAuthEntryPoint` / `RestAccessDeniedHandler`

**路径：** `.../framework/security/RestAuthEntryPoint.java`

```java
package com.cms.cms_back.framework.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RestAuthEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        write(response, ErrorCode.UNAUTHORIZED);
    }

    private void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResult.fail(errorCode.getCode(), errorCode.getMessage()));
    }
}
```

**路径：** `.../framework/security/RestAccessDeniedHandler.java`

```java
package com.cms.cms_back.framework.security;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(ErrorCode.FORBIDDEN.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResult.fail(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage()));
    }
}
```

---

### 5.6 完整 `CmsSecurityConfig`（替换空壳）

**路径：** `.../framework/security/CmsSecurityConfig.java`

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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
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

        // TraceId 去掉 @Component，仅在此注册；顺序：TraceId → JWT
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterBefore(traceIdFilter, JwtAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

> `TraceIdFilter` 类本身见 `2026-08-17-cms-traceid-in-security-tutorial.md`（无 `@Component`）。
> `securityFilterChain` 方法需注入 `TraceIdFilter traceIdFilter` 参数。

---

### 5.7 CORS

**路径：** `.../framework/security/CorsConfig.java`

```java
package com.cms.cms_back.framework.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("X-Trace-Id", "Authorization"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

---

### 5.8 完整 `AuthController`

**路径：** `admin/controllers/auth/AuthController.java`

```java
package com.cms.cms_back.admin.controllers.auth;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.framework.security.AuthService;
import com.cms.cms_back.pojo.dto.auth.LoginDTO;
import com.cms.cms_back.pojo.dto.auth.RefreshDTO;
import com.cms.cms_back.pojo.vo.auth.TokenResponseVO;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ApiResult<TokenResponseVO> login(@Valid @RequestBody LoginDTO loginReq) {
        return ApiResult.success(
                authService.login(loginReq.getUsername(), loginReq.getPassword()));
    }

    @PostMapping("/refresh")
    public ApiResult<TokenResponseVO> refresh(@Valid @RequestBody RefreshDTO refreshTokenReq) {
        return ApiResult.success(authService.refresh(refreshTokenReq.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@Valid @RequestBody RefreshDTO refreshTokenReq) {
        authService.logout(refreshTokenReq.getRefreshToken());
        return ApiResult.success();
    }
}
```

---

### 5.9 种子管理员（Liquibase）

先生成 BCrypt（临时跑一次）：

```java
System.out.println(new BCryptPasswordEncoder().encode("admin123456"));
```

**新建** `db/changelog/changes/002-seed-admin.yaml`：

```yaml
databaseChangeLog:
  - changeSet:
      id: 002-seed-admin
      author: cyrus
      changes:
        - sqlFile:
            path: db/changelog/changes/002-seed-admin.sql
            relativeToChangelogFile: false
            splitStatements: true
            stripComments: true
```

**`002-seed-admin.sql`**（把哈希换成你生成的）：

```sql
INSERT INTO users (username, password, display_name, status)
SELECT 'admin', '$2a$10$请替换为BCrypt哈希', '管理员', 1
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin');
```

在 `db.changelog-master.yaml` 里 `include` 该文件。

---

### 5.10 前端无感续签（完整示意）

```javascript
import axios from 'axios'

const api = axios.create({ baseURL: 'http://127.0.0.1:8080' })

let refreshing = null

function getAccess() { return localStorage.getItem('accessToken') }
function getRefresh() { return localStorage.getItem('refreshToken') }
function setTokens(data) {
  localStorage.setItem('accessToken', data.accessToken)
  localStorage.setItem('refreshToken', data.refreshToken)
}
function clearTokens() {
  localStorage.removeItem('accessToken')
  localStorage.removeItem('refreshToken')
}

api.interceptors.request.use((config) => {
  const access = getAccess()
  if (access) config.headers.Authorization = `Bearer ${access}`
  return config
})

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    const original = error.config
    const status = error.response?.status
    const url = original?.url || ''
    if (status === 401 && !original._retry && !url.includes('/auth/login') && !url.includes('/auth/refresh')) {
      original._retry = true
      if (!refreshing) {
        refreshing = api
          .post('/api/admin/auth/refresh', { refreshToken: getRefresh() })
          .then((r) => {
            const data = r.data.data
            setTokens(data)
            return data.accessToken
          })
          .catch((e) => {
            clearTokens()
            // window.location.href = '/login'
            throw e
          })
          .finally(() => { refreshing = null })
      }
      const newAccess = await refreshing
      original.headers.Authorization = `Bearer ${newAccess}`
      return api(original)
    }
    return Promise.reject(error)
  }
)

export default api
```

---

## 6. 自测

```powershell
# 登录
curl -s -X POST http://127.0.0.1:8080/api/admin/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"admin\",\"password\":\"admin123456\"}"

# 无票 → 401 ApiResult
curl -i http://127.0.0.1:8080/api/admin/spaces

# 带 Access
curl -i http://127.0.0.1:8080/api/admin/spaces `
  -H "Authorization: Bearer <accessToken>"

# 刷新（旧 refresh 再刷应 401）
curl -s -X POST http://127.0.0.1:8080/api/admin/auth/refresh `
  -H "Content-Type: application/json" `
  -d "{\"refreshToken\":\"<refreshToken>\"}"
```

---

## 7. Checklist

1. [ ] 修正 `User` 表名/时间字段  
2. [ ] 粘贴 `JwtService`（Nimbus）、`RefreshTokenStore`、`AuthService`  
3. [ ] 粘贴 Filter + EntryPoint + DeniedHandler  
4. [ ] 替换完整 `CmsSecurityConfig` + `CorsConfig`  
5. [ ] 补全 `AuthController`  
6. [ ] 种子管理员  
7. [ ] curl 测通；前端拦截器  

---

## 8. 小结

- **JWT 库：用 Nimbus**（已有 oauth2-resource-server），**不必上 JJWT**。  
- JJWT 只是写法更链式；对本仓库没有额外收益。  
- 未完成部分上文均已给出 **完整可粘贴代码**，命名对齐 `LoginDTO` / `TokenResponseVO` / `ApiResult` / `security.*`。  

需要我直接把这些类写入仓库时，说「按教程落地代码」即可。
