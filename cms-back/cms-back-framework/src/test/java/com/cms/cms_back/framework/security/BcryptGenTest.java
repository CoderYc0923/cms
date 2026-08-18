package com.cms.cms_back.framework.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class BcryptGenTest {

    @Test
    void printHash() {
        String raw = "test@123";
        String hash = new BCryptPasswordEncoder().encode(raw);
        System.out.println(hash);
        assert new BCryptPasswordEncoder().matches(raw, hash);
    }
}
