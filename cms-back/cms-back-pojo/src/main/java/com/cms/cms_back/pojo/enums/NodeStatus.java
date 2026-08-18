package com.cms.cms_back.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import lombok.Getter;

@Getter
public enum NodeStatus {

    VISIBLE(1, "展示"),
    HIDDEN(0, "隐藏");

    @EnumValue
    private final Integer code;
    private final String message;

    NodeStatus(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
