package com.cms.cms_back.admin.controllers.file;

import java.net.URI;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.cms.cms_back.common.api.ApiResult;
import com.cms.cms_back.framework.security.UserInfo;
import com.cms.cms_back.pojo.dto.upload.CompleteUploadDTO;
import com.cms.cms_back.pojo.dto.upload.InitUploadDTO;
import com.cms.cms_back.pojo.dto.upload.SignPartsDTO;
import com.cms.cms_back.pojo.vo.upload.CompleteUploadVO;
import com.cms.cms_back.pojo.vo.upload.InitUploadVO;
import com.cms.cms_back.pojo.vo.upload.SignPartsVO;
import com.cms.cms_back.system.service.UploadService;

import jakarta.validation.Valid;

/**
 * 文件上传控制器
 * AdminUploadController
 */
@RestController
@RequestMapping("/api/admin/files")
public class AdminUploadController {

    private final UploadService uploadService;

    public AdminUploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    /**
     * 初始化上传
     * 
     * @param initUploadDTO
     * @return
     */
    @PostMapping("/uploads/init")
    public ApiResult<InitUploadVO> init(@Valid @RequestBody InitUploadDTO initUploadDTO,
            @AuthenticationPrincipal UserInfo userInfo) {

        InitUploadVO vo = uploadService.init(initUploadDTO, userInfo.userId());

        return ApiResult.success(vo);
    }

    /**
     * 签发分片
     * 
     * @return
     */
    @PostMapping("/uploads/{fileId}/parts/sign")
    public ApiResult<SignPartsVO> signParts(@PathVariable Long fileId, @Valid @RequestBody SignPartsDTO signPartsDTO, @AuthenticationPrincipal UserInfo userInfo) {
        
        SignPartsVO vo = uploadService.signParts(fileId, signPartsDTO, userInfo.userId());
        return ApiResult.success(vo);
    }

    /**
     * 完成上传（分片上传和单文件上传）
     * 
     * @return
     */
    @PostMapping("/uploads/{fileId}/complete")
    public ApiResult<CompleteUploadVO> complete(@PathVariable Long fileId,
            @Valid @RequestBody(required = false) CompleteUploadDTO completeUploadDTO, @AuthenticationPrincipal UserInfo userInfo) {
        CompleteUploadVO vo = uploadService.complete(fileId, completeUploadDTO, userInfo.userId());
        return ApiResult.success(vo);
    }

    /**
     * 取消上传（分片上传）
     * 
     * @return
     */
    @PostMapping("/uploads/{fileId}/abort")
    public ApiResult<Void> abort(@PathVariable Long fileId, @AuthenticationPrincipal UserInfo userInfo) {
        uploadService.abort(fileId, userInfo.userId());
        return ApiResult.success(null);
    }

    /**
     * 获取文件内容
     * 
     * @return 返回302重定向到OSS 的短时签名 URL
     */
    @GetMapping("/{fileId}/content")
    public ResponseEntity<Void> getContent(@PathVariable Long fileId, @AuthenticationPrincipal UserInfo userInfo) {

        String signedUrl = uploadService.getContent(fileId, userInfo.userId(), false);

        return ResponseEntity
                .status(HttpStatus.FOUND) // 302
                .location(URI.create(signedUrl))
                .cacheControl(CacheControl.noStore()) // 不缓存
                .build();
    }
}
