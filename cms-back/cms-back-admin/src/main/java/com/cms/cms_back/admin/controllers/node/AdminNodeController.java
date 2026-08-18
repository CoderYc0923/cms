package com.cms.cms_back.admin.controllers.node;

import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.framework.security.UserInfo;
import com.cms.cms_back.pojo.dto.node.CreateNodeDTO;
import com.cms.cms_back.pojo.dto.node.UpdateNodeDTO;
import com.cms.cms_back.system.service.NodeService;

import jakarta.validation.Valid;

/**
 * 管理端节点（分组 / 菜单 / 文章节点）API 骨架。
 * 对应前端原 /api/groups、/api/items 写操作。
 */
@RestController
@RequestMapping("/api/admin/nodes")
public class AdminNodeController {

    private final NodeService nodeService;

    public AdminNodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @PostMapping
    public ApiResult<Void> create(@Valid @RequestBody CreateNodeDTO body, @AuthenticationPrincipal UserInfo user) {
        nodeService.create(body, user.userId());
        return ApiResult.success();
    }

    @PutMapping("/{id}")
    public ApiResult<Map<String, Object>> update(@PathVariable Long id, @Valid @RequestBody UpdateNodeDTO body) {
        nodeService.update(id, body);
        return ApiResult.success();
    }

    @DeleteMapping("/{id}")
    public ApiResult<Void> delete(@PathVariable Long id) {
        nodeService.delete(id);
        return ApiResult.success();
    }
}
