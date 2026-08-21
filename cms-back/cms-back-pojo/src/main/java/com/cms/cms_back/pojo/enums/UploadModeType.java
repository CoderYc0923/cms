package com.cms.cms_back.pojo.enums;

public enum UploadModeType {

    SINGLE("SINGLE", "单文件"),
    MULTIPART("MULTIPART", "分片上传");

    private String code;
    private String message;

    UploadModeType(String code, String message) {
        this.code = code;
        this.message = message;
    }
}
