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

@Service
public class JwtService {

    private final CmsSecurityProperties properties;
    private final byte[] secret;

    public JwtService(CmsSecurityProperties properties) {
        this.properties = properties;
        this.secret = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
    }

    /** 签发Access JWT: sub=userId, 自定义 claim username */
    public String createAccessToken(Long userId, String username) {
        Instant now = Instant.now();
        Instant exp = now.plus(properties.getAccessTokenTtl());
        try {
            /** 设置JWT Claims: 主题(用户ID)、自定义claim(用户名)、签发时间、过期时间 */
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
           throw new IllegalStateException("签发Access JWT失败", e);
        }
    }

    /** 获取Access Token过期时间（秒） */
    public long getAccessExpiresInSeconds() {
        return properties.getAccessTokenTtl().getSeconds();
    }

    /**
     * 解析并验证Access Token
     * @param accessToken
     * @return claims 声明
     * @throws ParseException 解析异常
     * @throws JOSEException 签名异常
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
