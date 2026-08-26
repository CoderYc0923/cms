package com.cms.cms_back.admin.task.media;

import org.springframework.stereotype.Component;

import com.cms.cms_back.system.service.MediaCleanupService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;

@Component
public class MediaCleanupJobHandler {;
    private final MediaCleanupService mediaCleanupService;

    public MediaCleanupJobHandler(MediaCleanupService mediaCleanupService) {
        this.mediaCleanupService = mediaCleanupService;
    }

    @XxlJob("mediaStaleUploadingCleanupJob")
    public void staleUploadingCleanupJob() {
        XxlJobHelper.log("start mediaStaleUploadingCleanupJob");
        int count = mediaCleanupService.cleanupStaleUploading();
        XxlJobHelper.log("finished mediaStaleUploadingCleanupJob, successCount=" + count);
    }

    @XxlJob("mediaOrphanReadyCleanupJob")
    public void orphanReadyCleanupJob() {
        XxlJobHelper.log("start mediaOrphanReadyCleanupJob");
        int count = mediaCleanupService.cleanupOrphanReady();
        XxlJobHelper.log("finished mediaOrphanReadyCleanupJob, successCount=" + count);
    }
}
