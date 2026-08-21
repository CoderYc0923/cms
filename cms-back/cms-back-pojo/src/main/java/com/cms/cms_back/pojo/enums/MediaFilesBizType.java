package com.cms.cms_back.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

@Getter
public enum MediaFilesBizType {
    ARTICLE_RICHTEXT("article_richtext", "文章富文本");

    @EnumValue
    @JsonValue
    private final String code;
    private final String message;

    MediaFilesBizType(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @JsonCreator
    public static MediaFilesBizType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MediaFilesBizType type : MediaFilesBizType.values()) {
            if (type.getCode().equals(code) || type.name().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
