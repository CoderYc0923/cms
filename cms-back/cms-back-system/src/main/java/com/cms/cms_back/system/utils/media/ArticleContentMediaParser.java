package com.cms.cms_back.system.utils.media;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文章内容媒体解析器
 * ArticleContentMediaParser
 */
public final class ArticleContentMediaParser {

    private static final Pattern STABLE_URL_PATTERM = Pattern.compile("/api/public/files/(\\d+)/content");

    private ArticleContentMediaParser() {}

    public static Set<Long> extractFileIds(String content) {
        if (content == null || content.isEmpty()) {
            return Set.of();
        }

        Matcher matcher = STABLE_URL_PATTERM.matcher(content);
        Set<Long> fileIds = new HashSet<>();
        while (matcher.find()) {
            fileIds.add(Long.parseLong(matcher.group(1)));
        }
        return fileIds;

    }
}
