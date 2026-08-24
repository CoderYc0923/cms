package com.cms.cms_back.admin.controllers.file;

import java.net.URI;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.cms_back.system.service.UploadService;

@RestController
@RequestMapping("/api/public/files")
public class PublicUploadController {

    private final UploadService uploadService;

    public PublicUploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * 获取公共文件内容
     * @param fileId
     * @return
     */
    @GetMapping("/{fileId}/content")
    public ResponseEntity<Void> getContent(@PathVariable Long fileId) {
        String signedUrl = uploadService.getContent(fileId, null, true);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(URI.create(signedUrl))
                .cacheControl(CacheControl.noStore())
                .build();
    }
}
