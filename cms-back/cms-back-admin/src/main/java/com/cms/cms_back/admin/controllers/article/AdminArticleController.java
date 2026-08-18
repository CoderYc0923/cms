package com.cms.cms_back.admin.controllers.article;

import java.util.Collections;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.pojo.vo.article.GetArticleVO;
import com.cms.cms_back.system.service.ArticleService;

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

    @GetMapping("/{id}")
    public ApiResult<GetArticleVO> get(@PathVariable Long id) {

        GetArticleVO vo = articleService.getArticle(id);

        return ApiResult.success(vo);
    }

    @PutMapping("/{id}")
    public ApiResult<Map<String, Object>> save(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return ApiResult.success(Collections.emptyMap());
    }

    @PostMapping("/{id}/publish")
    public ApiResult<Void> publish(@PathVariable Long id) {
        return ApiResult.success();
    }

    @PostMapping("/{id}/unpublish")
    public ApiResult<Void> unpublish(@PathVariable Long id) {
        return ApiResult.success();
    }
}
