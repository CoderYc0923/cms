package com.cms.cms_back.system.service.serviceImpl;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cms.cms_back.pojo.entity.MediaFiles;
import com.cms.cms_back.pojo.enums.MediaFilesStatus;
import com.cms.cms_back.system.mapper.MediaFilesMapper;
import com.cms.cms_back.system.oss.OssStorage;
import com.cms.cms_back.system.service.MediaCleanupService;
import com.cms.cms_back.system.task.media.MediaCleanupProperties;

@Service
public class MediaCleanupServiceImpl implements MediaCleanupService {

    private final MediaFilesMapper mediaFilesMapper;
    private final MediaCleanupProperties mediaCleanupProperties;
    private final OssStorage ossStorage;

    private static final Logger log = LoggerFactory.getLogger(MediaCleanupServiceImpl.class);

    public MediaCleanupServiceImpl(MediaFilesMapper mediaFilesMapper, MediaCleanupProperties mediaCleanupProperties,
            OssStorage ossStorage) {
        this.mediaFilesMapper = mediaFilesMapper;
        this.mediaCleanupProperties = mediaCleanupProperties;
        this.ossStorage = ossStorage;
    }

    /**
     * 清理上传超过指定小时未完成的文件
     */
    @Override
    public int cleanupStaleUploading() {
        if (!mediaCleanupProperties.isEnabled()) {
            log.info("Media cleanup is disabled");
            return 0;
        }

        /* 获取截止时间 */
        LocalDateTime deadline = LocalDateTime.now().minusHours(mediaCleanupProperties.getStaleUploadingAfterHours());

        List<MediaFiles> list = mediaFilesMapper.selectStaleUploading(deadline, mediaCleanupProperties.getBatchSize());

        int successCount = 0;

        for (MediaFiles mediaFile : list) {
            try {
                cleanupOnStaleUploading(mediaFile);
                successCount++;
            } catch (Exception e) {
                log.error("清理上传超过指定小时未完成的文件失败, fileId={}", mediaFile.getId(), e);
            }
        }

        log.info("清理上传超过指定小时未完成的文件成功, successCount={},deadline={},dryRun={}", successCount, deadline, mediaCleanupProperties.isDryRun());
        return successCount;
    }

    /**
     * 清理ready文件无引用超过指定天数将被删除的文件
     */
    @Override
    public int cleanupOrphanReady() {
        if (!mediaCleanupProperties.isEnabled()) {
            log.info("Media cleanup is disabled");
            return 0;
        }

        /* 获取截止时间 */
        LocalDateTime deadline = LocalDateTime.now().minusDays(mediaCleanupProperties.getOrphanReadyAfterDays());

        List<MediaFiles> list = mediaFilesMapper.selectOrphanReadyPrivate(deadline,
                mediaCleanupProperties.getBatchSize());

        int successCount = 0;

        for (MediaFiles mediaFile : list) {
            try {
                cleanupOnOrphanReady(mediaFile);
                successCount++;
            } catch (Exception e) {
                log.error("清理ready文件无引用超过指定天数将被删除的文件失败, fileId={}", mediaFile.getId(), e);
            }
        }

        log.info("清理ready文件无引用超过指定天数将被删除的文件成功, successCount={},deadline={},dryRun={}", successCount, deadline, mediaCleanupProperties.isDryRun());
        return successCount;
    }

    @Transactional(rollbackFor = Exception.class)
    protected void cleanupOnStaleUploading(MediaFiles mediaFile) {
        if (mediaCleanupProperties.isDryRun()) {
            log.info("[DRY RUN] 清理上传超过指定小时未完成的文件, fileId={}", mediaFile.getId());
            return;
        }

        if (StringUtils.hasText(mediaFile.getUploadId())) {
            ossStorage.abortMultipart(mediaFile.getObjectKey(), mediaFile.getUploadId());
        } else {
            try {
                ossStorage.deleteObject(mediaFile.getObjectKey());
            } catch (Exception e) {
                log.warn("删除文件失败, fileId={}", mediaFile.getId(), e);
            }
        }

        mediaFilesMapper.update(null,
                new LambdaUpdateWrapper<MediaFiles>()
                        .eq(MediaFiles::getId, mediaFile.getId())
                        .set(MediaFiles::getStatus, MediaFilesStatus.FAILED)
                        .set(MediaFiles::getUploadId, null));
    }

    @Transactional(rollbackFor = Exception.class)
    protected void cleanupOnOrphanReady(MediaFiles mediaFile) {
        if (mediaCleanupProperties.isDryRun()) {
            log.info("[DRY RUN] 清理ready文件无引用超过指定天数将被删除的文件, fileId={}", mediaFile.getId());
            return;
        }

        try {
            ossStorage.deleteObject(mediaFile.getObjectKey());
        } catch (Exception e) {
            log.warn("删除文件失败, fileId={}", mediaFile.getId(), e);
        }

        mediaFilesMapper.update(null,
            new LambdaUpdateWrapper<MediaFiles>()
                .eq(MediaFiles::getId, mediaFile.getId())
                .set(MediaFiles::getStatus, MediaFilesStatus.DELETED)
        );
    }
}

