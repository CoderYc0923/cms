package com.cms.cms_back.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import lombok.Getter;

@Getter
public enum SpaceStatus {

    ENABLED(1, "正常"),
    DISABLED(0, "禁用");

    @EnumValue
    private final Integer code;
    private final String message;

    SpaceStatus(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
