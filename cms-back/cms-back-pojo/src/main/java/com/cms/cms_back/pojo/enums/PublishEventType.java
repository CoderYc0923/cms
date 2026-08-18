package com.cms.cms_back.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import lombok.Getter;

@Getter
public enum PublishEventType {

    PUBLISHED("published", "发布"),
    UNPUBLISHED("unpublished", "下架"),
    UPDATED("updated", "更新");

    @EnumValue
    private final String code;
    private final String message;

    PublishEventType(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
