# CMS 鉴权体系教程：Spring Security + JWT 双 Token

**日期：** 2026-08-17（按当前仓库代码形态更新）  
**适用项目：** `cms-back`（Spring Boot 4.x 多模块）+ `cms-front`（admin / docs）  
**状态：** 方案已拍板；骨架已部分落地；鉴权核心逻辑待补全  
**前置：** MySQL + Redis + Liquibase + 全局异常（`ApiResult`）+ TraceIdFilter + MyBatis-Plus

---

## 0. 当前进度一览（对齐仓库）

| 项 | 状态 | 位置 |
|----|------|------|
| `ApiResult` 统一响应 | ✅ 已有 | `common/api/ApiResult.java` |
| `BizException` / `ErrorCode` | ✅ 已有 | `common/exception/` |
| `GlobalExceptionHandler` → `ApiResult` | ✅ 已有 | `framework/exception/` |
| `CmsSecurityProperties` + 启用 | ✅ 已有 | `framework/security/`，yml 前缀 `security` |
| Security / Redis / OAuth2-RS 依赖 | ✅ 已有 | `framework/pom.xml` |
| MyBatis-Plus（system starter + pojo annotation） | ✅ 已有 | `system` / `pojo` pom |
| `MybatisPlusConfig` + `@MapperScan` | ✅ 已有 | `framework/config/` |
| `User` / `UserStatus` / `UserMapper` | ✅ 已有 | pojo + system |
| `LoginDTO` / `RefreshDTO` / `TokenResponseVO` | ✅ 已有 | pojo dto/vo |
| `AuthController` 骨架 | ✅ 路径已对，方法体未实现 | `admin/controllers/auth/` |
| `JwtService` / `RefreshTokenStore` / `AuthService` | ❌ 待写 | 建议 `framework/security/` |
| `JwtAuthenticationFilter` + EntryPoint | ❌ 待写 | `framework/security/` |
| `SecurityFilterChain` 完整规则 | ❌ 待写 | 扩展现有 `CmsSecurityConfig` |
| JJWT 签发依赖（若自签 JWT） | ❌ 待加 | 见下文 |
| 种子管理员 Liquibase | ❌ 待加 | changelog |
| 前端无感续签拦截器 | ❌ 待做 | `cms-front` |

本文后续代码 **以当前命名为准**，不要再混用教程旧名（如 `LoginRequest`、`TokenResponse`、`cms.security`）。

---

## 1. 背景与目标

| 端 | 规则 |
|----|------|
| **admin** `/api/admin/**` | 必须登录；Token 无效 → **401** |
| **public** `/api/public/**` | 无需登录；草稿对 public → **404**（业务层） |
| MVP | 单一运营角色，不做复杂 RBAC |

统一响应（成功 / 失败同一形状）：

```json
{ "code": 200, "message": "success", "data": { } }
{ "code": 401, "message": "未登录或登录已失效", "data": null }
```

> 当前 `ApiResult.success` 成功码为 **200**（不是 0），失败码与 `ErrorCode` / HTTP 对齐。

---

## 2. 心智模型（简）

```text
请求
  → TraceIdFilter
  → Spring Security 链（JWT Bearer / 路径放行）
  → AuthController / 业务 Controller
  → Service（throw BizException）
  → GlobalExceptionHandler 或 Security EntryPoint
       ↓
     ApiResult
```

双 Token：

```text
Access  = JWT，30m，Header: Authorization: Bearer ...
Refresh = 随机串，7d，Redis；每次 /refresh 轮转（旧票作废）
无感续签 = 前端 401 时调 /refresh 再重试（后端只提供接口）
```

---

## 3. 已拍板约定

| 项 | 约定 |
|----|------|
| 配置前缀 | **`security.*`**（与现网 yml 一致，不是 `cms.security`） |
| Access | JWT HS256，TTL `security.access-token-ttl`（默认 30m） |
| Refresh | 不透明串 + Redis，TTL `security.refresh-token-ttl`（默认 7d），**轮转** |
| 密码 | BCrypt；列名 `password` 存哈希 |
| 成功体 | `ApiResult.success(data)` → code=200 |
| 失败体 | `ApiResult.fail(code, message)` + HTTP status |

### API

| 方法 | 路径 | 匿名 |
|------|------|------|
| POST | `/api/admin/auth/login` | 是 |
| POST | `/api/admin/auth/refresh` | 是 |
| POST | `/api/admin/auth/logout` | 建议带 refresh body；可 permitAll 或 authenticated |
| * | `/api/admin/**` | 否 |
| * | `/api/public/**` | 是 |
| GET | `/actuator/health` | 是 |

### Redis

```text
cms:auth:refresh:{refreshToken} → {"userId":1,"username":"admin"}
TTL = refresh 有效期
```

---

## 4. 模块与命名（当前风格）

```text
cms-back-common
  api/ApiResult.java
  exception/BizException.java, ErrorCode.java

cms-back-pojo
  dto/auth/LoginDTO.java, RefreshDTO.java          ← 入参
  vo/auth/TokenResponseVO.java                   ← 出参
  entity/User.java
  enums/UserStatus.java                          ← 包名 enums（不要用 enum）

cms-back-system
  mapper/UserMapper.java
  （可放 Auth 相关查询；编排也可放 framework）

cms-back-framework
  config/MybatisPlusConfig.java
  security/CmsSecurityProperties.java
  security/CmsSecurityConfig.java                ← 将扩展为完整 SecurityFilterChain
  exception/GlobalExceptionHandler.java
  web/TraceIdFilter.java
  security/JwtService.java                       ← 待写
  security/RefreshTokenStore.java                ← 待写
  security/AuthService.java                      ← 待写（推荐放这，少绕依赖）
  security/JwtAuthenticationFilter.java          ← 待写
  security/RestAuthEntryPoint.java               ← 待写

cms-back-admin
  controllersS/auth/AuthController.java           ← 注意：包名是 controllersS
```

依赖方向：`admin → framework → system → pojo → common`。

### Maven 落点（已按此落地）

| 依赖 | 模块 |
|------|------|
| `mybatis-plus-spring-boot4-starter` | **system** |
| `mybatis-plus-annotation` | **pojo** |
| `security` / `oauth2-resource-server` / `data-redis` | **framework** |
| mysql、liquibase、actuator | **admin** |

自签 JWT 时再在 **framework** 增加 JJWT：

```xml
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>0.12.6</version>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-impl</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>
<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-jackson</artifactId>
  <version>0.12.6</version>
  <scope>runtime</scope>
</dependency>
```

（也可用 Nimbus 自签；JJWT 更直观。`oauth2-resource-server` 可保留或去掉，二选一即可。）

---

## 5. 已落地代码（以仓库为准，勿重复造同名类）

### 5.1 配置 `application.yml`

```yaml
security:
  jwt-secret: ${CMS_JWT_SECRET:please-change-me-to-a-very-long-secret-key-32bytes}
  access-token-ttl: 30m
  refresh-token-ttl: 7d
```

`application-local.yml`：

```yaml
security:
  jwt-secret: cms-jwt-secret-for-local-2026@123!
```

启动：

```powershell
mvn spring-boot:run -pl cms-back-admin "-Dspring-boot.run.profiles=local"
```

### 5.2 `CmsSecurityProperties`（已有）

```java
@ConfigurationProperties(prefix = "security")
@Data
public class CmsSecurityProperties {
    private String jwtSecret;
    private Duration accessTokenTtl = Duration.ofMinutes(30);
    private Duration refreshTokenTtl = Duration.ofDays(7);
}
```

```java
@Configuration
@EnableConfigurationProperties(CmsSecurityProperties.class)  // 必须是 Properties，不是 Config 自己
public class CmsSecurityConfig {
}
```

### 5.3 `ApiResult`（已有）

```java
public static <T> ApiResult<T> success(T data) {
    return new ApiResult<>(200, "success", data);
}
public static <T> ApiResult<T> fail(int code, String message) {
    return new ApiResult<>(code, message, null);
}
```

### 5.4 DTO / VO（已有）

- `LoginDTO`：`username` + `@Size(min=8, max=16)` 的 `password`  
- `RefreshDTO`：`refreshToken`  
- `TokenResponseVO`：`accessToken` / `refreshToken` / `@Builder.Default tokenType="Bearer"` / `expiresIn`

### 5.5 `User` + `UserStatus`（已有，有两处建议修正）

```java
@TableName("user")           // ⚠️ Liquibase 表名是 users，建议改成 "users"
private UserStatus status;    // @EnumValue 在枚举 code 上，正确；实体用枚举不需要 @JsonCreator
private LocalDateTime createAt;  // ⚠️ 列是 created_at → 建议字段 createdAt
private LocalDateTime updateAt;  // ⚠️ 建议 updatedAt
```

`UserStatus`：包名 `pojo.enums`，`@EnumValue` 标在 `code` 上即可做 DB 互转。

### 5.6 `AuthController`（已有骨架，待接 Service）

```java
@RestController
@RequestMapping("/api/admin/auth")
public class AuthController {

    @PostMapping("/login")
    public ApiResult<TokenResponseVO> login(@Valid @RequestBody LoginDTO loginReq) {
        return null; // TODO → ApiResult.success(authService.login(...))
    }

    @PostMapping("/refresh")
    public ApiResult<TokenResponseVO> refresh(@Valid @RequestBody RefreshDTO refreshTokenReq) {
        return null;
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@Valid @RequestBody RefreshDTO refreshTokenReq) {
        return null;
    }
}
```

目标实现：

```java
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ApiResult<TokenResponseVO> login(@Valid @RequestBody LoginDTO loginReq) {
        return ApiResult.success(
                authService.login(loginReq.getUsername(), loginReq.getPassword()));
    }

    @PostMapping("/refresh")
    public ApiResult<TokenResponseVO> refresh(@Valid @RequestBody RefreshDTO req) {
        return ApiResult.success(authService.refresh(req.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@Valid @RequestBody RefreshDTO req) {
        authService.logout(req.getRefreshToken());
        return ApiResult.success();
    }
}
```

### 5.7 异常处理（已统一 `ApiResult`）

```java
return ResponseEntity.status(httpStatus).body(ApiResult.fail(code, message));
```

Security 未认证 **不保证**进 Advice，要用 `AuthenticationEntryPoint` 同样写 `ApiResult.fail(401, ...)`。

---

## 6. 待实现：完整示例代码（按当前风格）

### 6.1 `JwtService`

路径：`framework/security/JwtService.java`

```java
package com.cms.cms_back.framework.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    private final CmsSecurityProperties properties;
    private final SecretKey key;

    public JwtService(CmsSecurityProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getAccessTokenTtl());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key)
                .compact();
    }

    public long getAccessExpiresInSeconds() {
        return properties.getAccessTokenTtl().toSeconds();
    }

    public Claims parseAndValidate(String accessToken) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();
    }
}
```

### 6.2 `RefreshTokenStore`

路径：`framework/security/RefreshTokenStore.java`

```java
package com.cms.cms_back.framework.security;

import java.time.Duration;
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

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
}
```

### 6.3 `AuthService`

路径：`framework/security/AuthService.java`

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

    public TokenResponseVO refresh(String refreshToken) {
        var session = refreshTokenStore.find(refreshToken)
                .orElseThrow(() -> BizException.unauthorized("登录已失效，请重新登录"));
        refreshTokenStore.revoke(refreshToken); // 轮转
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

### 6.4 `JwtAuthenticationFilter`

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

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
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
                Claims claims = jwtService.parseAndValidate(token);
                Long userId = Long.valueOf(claims.getSubject());
                String username = claims.get("username", String.class);
                var auth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
                auth.setDetails(username);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
```

### 6.5 `RestAuthEntryPoint`（401 JSON = ApiResult）

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
        response.setStatus(ErrorCode.UNAUTHORIZED.getHttpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getOutputStream(),
                ApiResult.fail(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage()));
    }
}
```

（`RestAccessDeniedHandler` 同理，用 `ErrorCode.FORBIDDEN`。）

### 6.6 扩展 `CmsSecurityConfig`（SecurityFilterChain）

把现有空 `CmsSecurityConfig` 扩成：

```java
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(CmsSecurityProperties.class)
public class CmsSecurityConfig {

    // 注入 JwtAuthenticationFilter、RestAuthEntryPoint、RestAccessDeniedHandler

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/admin/auth/login", "/api/admin/auth/refresh").permitAll()
                .requestMatchers("/api/admin/auth/logout").permitAll() // 或 authenticated，按产品定
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/admin/**").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .cors(Customizer.withDefaults());

        // TraceIdFilter：若已是 @Component，不要再 addFilter，避免执行两次
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

---

## 7. 前端无感续签（admin）

```javascript
// 401 → 单飞 refresh → 重试原请求；refresh 失败 → 跳登录
// Authorization: Bearer <accessToken>
// body: { refreshToken }
```

与后端路径：`POST /api/admin/auth/refresh`，响应：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "accessToken": "...",
    "refreshToken": "...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

---

## 8. 自测

```powershell
curl -s -X POST http://127.0.0.1:8080/api/admin/auth/login `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"admin\",\"password\":\"admin123456\"}"

curl -i http://127.0.0.1:8080/api/admin/spaces
# 期望 401 + ApiResult

curl -i http://127.0.0.1:8080/api/admin/spaces `
  -H "Authorization: Bearer <accessToken>"
```

---

## 9. 接下来 Checklist（只列未完成）

1. [ ] framework 增加 JJWT（或选定 Nimbus 签发方案）  
2. [ ] 修正 `User`：`@TableName("users")`，`createdAt` / `updatedAt`  
3. [ ] 实现 `JwtService`、`RefreshTokenStore`、`AuthService`  
4. [ ] 实现 `JwtAuthenticationFilter`、`RestAuthEntryPoint`（及 DeniedHandler）  
5. [ ] 完善 `CmsSecurityConfig` 的 `SecurityFilterChain` + `PasswordEncoder`  
6. [ ] `AuthController` 注入 Service，返回 `ApiResult.success(...)`  
7. [ ] Liquibase 种子管理员（BCrypt）  
8. [ ] curl 测通 login / 401 / refresh 轮转 / logout  
9. [ ] admin 前端拦截器无感续签  

---

## 10. 和旧版教程的差异（避免照抄过期片段）

| 旧教程 | 当前仓库 |
|--------|----------|
| `cms.security` | **`security`** |
| `LoginRequest` / `TokenResponse` | **`LoginDTO` / `TokenResponseVO`** |
| `ApiResponse.ok`，成功 code=0 | **`ApiResult.success`，成功 code=200** |
| Controller 包 `controller` | **`controllers.auth`** |
| 从零写 SecurityConfig | **已有 `CmsSecurityConfig`，在其上扩展** |
| 异常返回 `Map` | **已统一 `ApiResult`** |

---

**文档路径：** `cms/2026-08-17-cms-auth-spring-security-jwt-tutorial.md`  
按第 9 节 checklist 继续即可；需要把待实现类直接写入仓库时，说「按更新后的教程落地代码」。
