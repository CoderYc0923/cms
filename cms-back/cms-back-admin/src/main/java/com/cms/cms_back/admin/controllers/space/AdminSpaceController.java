package com.cms.cms_back.admin.controllers.space;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.pojo.vo.space.SpaceVO;
import com.cms.cms_back.pojo.dto.space.CreateSpaceDTO;
import com.cms.cms_back.pojo.dto.space.UpdateSpaceDTO;
import com.cms.cms_back.pojo.vo.space.SpaceNodeTreeVO;
import com.cms.cms_back.system.service.SpaceService;

import jakarta.validation.Valid;

/**
 * 管理端 Space API 骨架。
 */
@RestController
@RequestMapping("/api/admin/spaces")
public class AdminSpaceController {

    private final SpaceService spaceService;

    public AdminSpaceController(SpaceService spaceService) {
        this.spaceService = spaceService;
    }

    @GetMapping
    public ApiResult<List<SpaceVO>> list(@RequestParam(required = false) Integer status) {
        List<SpaceVO> list = spaceService.getList(status);

        return ApiResult.success(list);
    }

    @PostMapping
    public ApiResult<Void> create(@Valid @RequestBody CreateSpaceDTO dto) {
        spaceService.create(dto);

        return ApiResult.success();
    }

    @PutMapping("/{id}")
    public ApiResult<Void> update(@PathVariable Long id, @Valid @RequestBody UpdateSpaceDTO dto) {
        spaceService.update(id, dto);

        return ApiResult.success();
    }

    /** 完整目录树（含草稿），slug 对应前端原 source，如 shopchup / iot */
    @GetMapping("/{slug}/tree")
    public ApiResult<List<SpaceNodeTreeVO>> tree(@PathVariable String slug) {
        List<SpaceNodeTreeVO> tree = spaceService.getTree(slug);

        return ApiResult.success(tree);
    }
}
