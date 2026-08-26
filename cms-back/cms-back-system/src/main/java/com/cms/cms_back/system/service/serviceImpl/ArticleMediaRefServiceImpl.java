package com.cms.cms_back.system.service.serviceImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.pojo.entity.Article;
import com.cms.cms_back.pojo.entity.ArticleMediaRefs;
import com.cms.cms_back.pojo.entity.MediaFiles;
import com.cms.cms_back.pojo.enums.MediaFilesAccessLevelType;
import com.cms.cms_back.pojo.enums.MediaFilesStatus;
import com.cms.cms_back.system.mapper.ArticleMediaRefMapper;
import com.cms.cms_back.system.mapper.MediaFilesMapper;
import com.cms.cms_back.system.service.ArticleMediaRefService;
import com.cms.cms_back.system.utils.media.ArticleContentMediaParser;

@Service
public class ArticleMediaRefServiceImpl implements ArticleMediaRefService {

    private final MediaFilesMapper mediaFilesMapper;
    private final ArticleMediaRefMapper articleMediaRefMapper;

    private static final Logger log = LoggerFactory.getLogger(ArticleMediaRefServiceImpl.class);

    public ArticleMediaRefServiceImpl(MediaFilesMapper mediaFilesMapper, ArticleMediaRefMapper articleMediaRefMapper) {
        this.mediaFilesMapper = mediaFilesMapper;
        this.articleMediaRefMapper = articleMediaRefMapper;
    }

    /**
     * 获取某篇文章引用的所有文件ID
     * 
     * @param articleId
     */
    @Override
    public List<Long> getFileIdsByArticleId(Long articleId) {
        if (articleId == null || articleId <= 0) {
            throw BizException.badRequest("文章ID不能为空");
        }

        return articleMediaRefMapper.selectFileIdsByArticleId(articleId);
    }

    /**
     * 删除某篇文章引用的所有文件引用关系
     * 
     * @param articleId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRefsByArticleId(Long articleId) {
        List<Long> fileIds = getFileIdsByArticleId(articleId);

        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        articleMediaRefMapper.delete(
                new LambdaQueryWrapper<ArticleMediaRefs>()
                        .in(ArticleMediaRefs::getFileId, fileIds)
                        .eq(ArticleMediaRefs::getArticleId, articleId));

        /** 删除后需要重算文件访问级别 */
        recomputeAccessLevel(new HashSet<>(fileIds));

    }

    /**
     * 根据正文 diff 引用关系，返回受影响的fileId
     * 
     * @param articleId
     * @param spaceId
     * @param content
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Set<Long> syncRefsByContent(Long articleId, Long spaceId, String content) {
        if (articleId == null || articleId <= 0) {
            throw BizException.badRequest("文章ID不能为空");
        }

        /** 提取正文中的文件ID */
        Set<Long> newFileIds = ArticleContentMediaParser.extractFileIds(content);
        validateFiles(newFileIds, spaceId);

        /** 获取库中文章当前引用的文件ID */
        List<Long> oldFileIdList = articleMediaRefMapper.selectFileIdsByArticleId(articleId);
        Set<Long> oldFileIds = new HashSet<>(oldFileIdList);

        /** 过滤出需要新增的文件ID */
        Set<Long> toAdd = new HashSet<>(newFileIds);
        toAdd.removeAll(oldFileIds);

        /** 过滤出需要删除的文件ID */
        Set<Long> toRemove = new HashSet<>(oldFileIds);
        toRemove.removeAll(newFileIds);

        for (Long fileId : toAdd) {
            try {
                ArticleMediaRefs ref = new ArticleMediaRefs();
                ref.setArticleId(articleId);
                ref.setFileId(fileId);
                articleMediaRefMapper.insert(ref);
            } catch (DuplicateKeyException e) {
                // 并发或重复保存，视为成功
                log.debug("文章{}引用的文件{}已存在，视为成功", articleId, fileId);
            }

        }

        if (!toRemove.isEmpty()) {
            articleMediaRefMapper.delete(
                    new LambdaQueryWrapper<ArticleMediaRefs>()
                            .eq(ArticleMediaRefs::getArticleId, articleId)
                            .in(ArticleMediaRefs::getFileId, toRemove));
        }

        /** 返回受影响的文件ID */
        Set<Long> affectedFileIds = new HashSet<>();
        affectedFileIds.addAll(oldFileIds);
        affectedFileIds.addAll(newFileIds);
        return affectedFileIds;
    }

    /**
     * 重算文件访问级别
     * 
     * @param fileIds
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recomputeAccessLevel(Set<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }

        /** 拿到库中已经发布的文件ID引用列表 */
        List<Long> shouldPublicList = articleMediaRefMapper.selectPublishedFileIds(fileIds);
        Set<Long> shouldPublic = new HashSet<>(shouldPublicList);

        /** 本次fileIds中不属于已发布文件的ID集合（需要设置为私密） */
        Set<Long> shouldPrivate = new HashSet<>(fileIds);
        shouldPrivate.removeAll(shouldPublic);

        if (!shouldPublic.isEmpty()) {
            mediaFilesMapper.update(null,
                    new LambdaUpdateWrapper<MediaFiles>()
                            .in(MediaFiles::getId, shouldPublic)
                            .eq(MediaFiles::getStatus, MediaFilesStatus.READY)
                            .set(MediaFiles::getAccessLevel, MediaFilesAccessLevelType.PUBLIC));
        }

        if (!shouldPrivate.isEmpty()) {
            mediaFilesMapper.update(null,
                    new LambdaUpdateWrapper<MediaFiles>()
                            .in(MediaFiles::getId, shouldPrivate)
                            .eq(MediaFiles::getStatus, MediaFilesStatus.READY)
                            .set(MediaFiles::getAccessLevel, MediaFilesAccessLevelType.PRIVATE));
        }

        log.info("重算文件访问级别成功, publicSize: {}, privateSize: {}", shouldPublic.size(), shouldPrivate.size());
    }

    /**
     * 重算某篇文章当前引用的文件的访问级别
     * 
     * @param articleId
     */
    @Override
    public void recomputeAccessLevelForArticle(Long articleId) {
        List<Long> fileIds = articleMediaRefMapper.selectFileIdsByArticleId(articleId);
        recomputeAccessLevel(new HashSet<>(fileIds));
    }

    /**
     * 重算某篇文章受影响的文件访问级别
     * 
     * @param article
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recomputeAccessLevelForArticleDiff(Article article) {
        Set<Long> affectedFileIds = syncRefsByContent(article.getId(), article.getSpaceId(), article.getContent());
        recomputeAccessLevel(affectedFileIds);
    }

    /**
     * 验证文件安全性
     * 
     * @param fileIds
     * @param spaceId
     */
    private void validateFiles(Set<Long> fileIds, Long spaceId) {
        if (fileIds.isEmpty()) {
            return;
        }
        List<MediaFiles> files = mediaFilesMapper.selectByIds(fileIds);
        if (files.size() != fileIds.size()) {
            throw BizException.badRequest("文件不存在");
        }
        for (MediaFiles file : files) {
            if (file.getStatus() != MediaFilesStatus.READY) {
                throw BizException.badRequest("文件状态异常");
            }
            if (spaceId != null && file.getSpaceId() != null && !file.getSpaceId().equals(spaceId)) {
                throw BizException.badRequest("文件空间不匹配");
            }
        }
    }
}
