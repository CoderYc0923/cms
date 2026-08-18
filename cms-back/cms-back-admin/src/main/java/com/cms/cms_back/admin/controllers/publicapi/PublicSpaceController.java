package com.cms.cms_back.admin.controllers.publicapi;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.cms_back.common.api.ApiResult;

/**
 * 公开只读 API 骨架（docs / 未登录可读树）。
 * 返回空数据，仅保证路由可达；后续再接 Service。
 */
@RestController
@RequestMapping("/api/public/spaces")
public class PublicSpaceController {

    @GetMapping
    public ApiResult<List<Map<String, Object>>> listSpaces() {
        return ApiResult.success(Collections.emptyList());
    }

    @GetMapping("/{slug}/tree")
    public ApiResult<List<Map<String, Object>>> tree(@PathVariable String slug) {
        return ApiResult.success(Collections.emptyList());
    }

    @GetMapping("/{slug}/articles/{id}")
    public ApiResult<Map<String, Object>> article(@PathVariable String slug, @PathVariable Long id) {
        return ApiResult.success(null);
    }
}
