package com.cms.cms_back.admin.controllers.file;

import java.util.Collections;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.cms.cms_back.common.api.ApiResult;

/**
 * 管理端文件上传/预览骨架（富文本插图等）。
 */
@RestController
@RequestMapping("/api/admin/files")
public class AdminFileController {

    @PostMapping("/upload")
    public ApiResult<Map<String, Object>> upload(
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) String accountId,
            @RequestParam(value = "file", required = false) MultipartFile file) {
        return ApiResult.success(Collections.emptyMap());
    }

    @GetMapping("/get")
    public ApiResult<Map<String, Object>> get(@RequestParam Map<String, String> params) {
        return ApiResult.success(Collections.emptyMap());
    }

    @GetMapping("/preview")
    public ApiResult<Map<String, Object>> preview(@RequestParam Map<String, String> params) {
        return ApiResult.success(Collections.emptyMap());
    }
}
