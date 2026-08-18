package com.cms.cms_back.admin.controllers.space;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.cms_back.common.api.ApiResult;

/**
 * 管理端 Space API 骨架。
 */
@RestController
@RequestMapping("/api/admin/spaces")
public class AdminSpaceController {

    @GetMapping
    public ApiResult<List<Map<String, Object>>> list() {
        return ApiResult.success(Collections.emptyList());
    }

    @PostMapping
    public ApiResult<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        return ApiResult.success(Collections.emptyMap());
    }

    @PutMapping("/{id}")
    public ApiResult<Map<String, Object>> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResult.success(Collections.emptyMap());
    }

    /** 完整目录树（含草稿），slug 对应前端原 source，如 shopchup / iot */
    @GetMapping("/{slug}/tree")
    public ApiResult<List<Map<String, Object>>> tree(@PathVariable String slug) {
        return ApiResult.success(Collections.emptyList());
    }
}
