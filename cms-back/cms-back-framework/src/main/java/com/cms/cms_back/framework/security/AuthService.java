package com.cms.cms_back.framework.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.pojo.entity.User;
import com.cms.cms_back.pojo.vo.auth.TokenResponseVO;
import com.cms.cms_back.system.mapper.UserMapper;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenStore refreshTokenStore;
    
    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtService jwtService, RefreshTokenStore refreshTokenStore) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenStore = refreshTokenStore;
    }

    /**
     * 登录
     * @param username
     * @param password
     * @return 令牌响应
     */
    public TokenResponseVO login(String username, String password) {
        User user = userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
                .last("limit 1")
        );
        if (user == null) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw BizException.unauthorized("用户名或密码错误");
        }
        return issueTokens(user.getId(), user.getUsername());
    }

    /**
     * 刷新令牌
     * @param refreshToken
     * @return 令牌响应
     */
    public TokenResponseVO refresh(String refreshToken) {
        var session = refreshTokenStore.find(refreshToken).orElseThrow(() -> BizException.unauthorized("登录失效，请重新登录"));
        refreshTokenStore.revoke(refreshToken);
        return issueTokens(session.getUserId(), session.getUsername());
    }

    /**
     * 登出
     * @param refreshToken
     */
    public void logout(String refreshToken) {
        refreshTokenStore.revoke(refreshToken);
    }

    /**
     * 颁发令牌
     * @param userId
     * @param username
     * @return
     */
    private TokenResponseVO issueTokens(Long userId, String username) {
        return TokenResponseVO.builder()
            .accessToken(jwtService.createAccessToken(userId, username))
            .refreshToken(refreshTokenStore.issue(userId, username))
            .expiresIn(jwtService.getAccessExpiresInSeconds())
            .build();
    }
}
