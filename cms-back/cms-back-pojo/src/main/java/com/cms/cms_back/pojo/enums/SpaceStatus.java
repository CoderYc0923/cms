package com.cms.cms_back.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

@Getter
public enum SpaceStatus {

    ENABLED(1, "正常"),
    DISABLED(0, "禁用");

    @EnumValue
    @JsonValue
    private final Integer code;
    private final String message;

    SpaceStatus(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    @JsonCreator
    public static SpaceStatus formCode(Integer code) {

        if (code == null) {
            return null;
        }

        for (SpaceStatus status : SpaceStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }

        return null;
    }
}
