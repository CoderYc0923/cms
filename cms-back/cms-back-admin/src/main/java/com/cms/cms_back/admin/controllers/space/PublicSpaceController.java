package com.cms.cms_back.admin.controllers.space;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.pojo.vo.article.GetArticleVO;
import com.cms.cms_back.pojo.vo.space.SpaceNodeTreeVO;
import com.cms.cms_back.system.service.ArticleService;
import com.cms.cms_back.system.service.SpaceService;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/public/spaces")
public class PublicSpaceController {

    private final SpaceService spaceService;
    private final ArticleService articleService;

    public PublicSpaceController(SpaceService spaceService, ArticleService articleService) {
        this.spaceService = spaceService;
        this.articleService = articleService;
    }

    @GetMapping("/{slug}/tree")
    public ApiResult<List<SpaceNodeTreeVO>> getTree(@PathVariable String slug) {
        List<SpaceNodeTreeVO> tree = spaceService.getTree(slug);
        return ApiResult.success(tree);
    }

    @GetMapping("/{slug}/articles/{id}")
    public ApiResult<GetArticleVO> getArticle(@PathVariable String slug, @PathVariable Long id) {
        GetArticleVO vo = articleService.getPublicArticle(slug, id);
        return ApiResult.success(vo);
    }
}
