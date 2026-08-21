package com.cms.cms_back.pojo.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import lombok.Getter;

@Getter
public enum MediaFilesStatus {
    UPLOADING("uploading", "上传中"),
    READY("ready", "已上传"),
    FAILED("failed", "上传失败"),
    DELETED("deleted", "已删除");

    @EnumValue
    @JsonValue
    private final String code;
    private final String message;

    MediaFilesStatus(String code, String message) {
        this.code = code;
        this.message = message;
    }

    @JsonCreator
    public static MediaFilesStatus fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (MediaFilesStatus type : MediaFilesStatus.values()) {
            if (type.getCode().equals(code) || type.name().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
