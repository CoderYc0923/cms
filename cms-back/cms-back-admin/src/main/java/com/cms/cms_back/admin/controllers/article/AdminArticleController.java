package com.cms.cms_back.admin.controllers.article;

import java.util.Collections;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.framework.security.UserInfo;
import com.cms.cms_back.pojo.dto.article.CreateArticleDTO;
import com.cms.cms_back.pojo.dto.article.SaveArticleDTO;
import com.cms.cms_back.pojo.vo.article.GetArticleVO;
import com.cms.cms_back.system.service.ArticleService;

import jakarta.validation.Valid;

/**
 * 管理端文章正文 / 发布 API 骨架。
 */
@RestController
@RequestMapping("/api/admin/articles")
public class AdminArticleController {

    private final ArticleService articleService;

    public AdminArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    /**
     * 发布文章
     * 
     * @param id 文章ID
     * @return
     */
    @PostMapping("/{id}/publish")
    public ApiResult<Void> publish(@PathVariable Long id, @AuthenticationPrincipal UserInfo user) {
        articleService.publish(id, user.userId());
        return ApiResult.success();
    }

    /**
     * 取消发布文章
     * 
     * @param id
     * @return
     */
    @PostMapping("/{id}/unpublish")
    public ApiResult<Void> unpublish(@PathVariable Long id, @AuthenticationPrincipal UserInfo user) {
        articleService.unpublish(id, user.userId());
        return ApiResult.success();
    }

    /**
     * 获取文章正文
     * 
     * @param id 文章ID
     * @return 文章正文
     */
    @GetMapping("/{id}")
    public ApiResult<GetArticleVO> get(@PathVariable Long id) {

        GetArticleVO vo = articleService.getArticle(id);

        return ApiResult.success(vo);
    }

    /**
     * 保存文章正文
     * 
     * @param nodeId   文章节点ID
     * @param body 文章正文
     * @return 文章正文
     */
    @PutMapping("/{nodeId}")
    public ApiResult<Void> save(@PathVariable Long nodeId, @Valid @RequestBody SaveArticleDTO body,
            @AuthenticationPrincipal UserInfo user) {
        articleService.save(nodeId, body, user.userId());
        return ApiResult.success();
    }
}
