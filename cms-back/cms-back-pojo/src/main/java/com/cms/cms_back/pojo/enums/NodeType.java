package com.cms.cms_back.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import lombok.Getter;

@Getter
public enum NodeType {

    GROUP("group", "分组"),
    MENU("menu", "菜单"),
    ARTICLE("article", "文章");

    @EnumValue
    private final String code;
    private final String message;

    NodeType(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
