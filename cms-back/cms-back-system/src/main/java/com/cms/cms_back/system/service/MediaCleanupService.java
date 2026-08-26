package com.cms.cms_back.system.service;

public interface MediaCleanupService {

    /**
     * 清理上传超过指定小时未完成的文件
     * @return
     */
    int cleanupStaleUploading();

    /**
     * 清理ready文件无引用超过指定天数将被删除的文件
     * @return
     */
    int cleanupOrphanReady();
}
