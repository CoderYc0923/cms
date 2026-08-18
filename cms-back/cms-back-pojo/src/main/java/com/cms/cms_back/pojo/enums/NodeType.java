package com.cms.cms_back.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

@Getter
public enum NodeType {

    GROUP("group", "分组"),
    MENU("menu", "菜单"),
    ARTICLE("article", "文章");

    @EnumValue
    @JsonValue
    private final String code;
    private final String message;

    NodeType(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @JsonCreator
    public static NodeType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (NodeType nodeType : NodeType.values()) {
            if (nodeType.getCode().equals(code) || nodeType.name().equals(code)) {
                return nodeType;
            }
        }
        return null;
    }
}
