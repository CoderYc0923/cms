package com.cms.cms_back.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

@Getter
public enum MediaFilesAccessLevelType {
    PRIVATE("private", "私有"),
    PUBLIC("public", "公开");

    @EnumValue
    @JsonValue
    private final String code;
    private final String message;

    MediaFilesAccessLevelType(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @JsonCreator
    public static MediaFilesAccessLevelType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MediaFilesAccessLevelType type : MediaFilesAccessLevelType.values()) {
            if (type.getCode().equals(code) || type.name().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
