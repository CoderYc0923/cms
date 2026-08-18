package com.cms.cms_back.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import lombok.Getter;

@Getter
public enum PublishStatus {

    DRAFT("draft", "草稿"),
    PUBLISHED("published", "已发布");

    @EnumValue
    private final String code;
    private final String message;

    PublishStatus(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
