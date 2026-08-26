package com.cms.cms_back.system.task.media;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "cms.media-cleanup")
public class MediaCleanupProperties {

    /** 是否启用媒体清理 */
    private boolean enabled;

    /**
     * 每次处理文件数量
     * 默认200
     */
    private int batchSize = 200;

    /**
     * uploading超过指定小时未完成的文件将被删除
     * 默认24小时
     */
    private int staleUploadingAfterHours = 24;

    /**
     * ready文件无引用超过指定天数将被删除
     * 默认7天
     */
    private int orphanReadyAfterDays = 7;

    /** 是否只打印日志不删除文件 */
    private boolean dryRun = false;
}
