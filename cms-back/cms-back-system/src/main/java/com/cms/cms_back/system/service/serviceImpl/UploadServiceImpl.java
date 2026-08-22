package com.cms.cms_back.system.service.serviceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.cms_back.system.mapper.MediaFilesMapper;
import com.cms.cms_back.system.oss.OssProperties;
import com.cms.cms_back.system.oss.OssStorage;
import com.cms.cms_back.system.service.UploadService;
import com.cms.cms_back.pojo.vo.upload.InitUploadVO;
import com.cms.cms_back.pojo.dto.upload.InitUploadDTO;
import com.cms.cms_back.pojo.vo.upload.SignPartsVO;
import com.cms.cms_back.pojo.dto.upload.SignPartsDTO;
import com.cms.cms_back.pojo.entity.MediaFiles;
import com.cms.cms_back.pojo.enums.MediaFilesAccessLevelType;
import com.cms.cms_back.pojo.enums.MediaFilesBizType;
import com.cms.cms_back.pojo.enums.MediaFilesStatus;
import com.cms.cms_back.pojo.enums.UploadModeType;
import com.cms.cms_back.pojo.vo.upload.CompleteUploadVO;
import com.cms.cms_back.pojo.vo.upload.InitSingleHeaderVO;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.common.exception.ErrorCode;
import com.cms.cms_back.pojo.dto.upload.CompleteUploadDTO;

@Service
public class UploadServiceImpl implements UploadService {

    private final MediaFilesMapper mediaFilesMapper;
    private final OssProperties ossProperties;
    private final OssStorage ossStorage;

    private static final Logger log = LoggerFactory.getLogger(UploadServiceImpl.class);
    private static final Map<String, String> CONTENT_TYPE_EXTENSION_MAP = Map.of(
            "image/jpeg", ".jpg",
            "image/png", ".png",
            "image/gif", ".gif",
            "image/webp", ".webp",
            "video/mp4", ".mp4",
            "video/webm", ".webm");

    public UploadServiceImpl(MediaFilesMapper mediaFilesMapper, OssProperties ossProperties, OssStorage ossStorage) {
        this.mediaFilesMapper = mediaFilesMapper;
        this.ossProperties = ossProperties;
        this.ossStorage = ossStorage;
    }

    /**
     * 初始化上传
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public InitUploadVO init(InitUploadDTO initUploadDTO, Long userId) {
        validateInitUploadDTO(initUploadDTO);

        MediaFilesBizType bizType = MediaFilesBizType.fromCode(initUploadDTO.getBizType());
        if (bizType == null) {
            throw BizException.badRequest("无效的业务类型");
        }

        String objectKey = generateObjectKey(bizType, initUploadDTO.getContentType());

        boolean isMultiPart = initUploadDTO.getSizeBytes() >= ossProperties.getMultipartThresholdBytes();

        MediaFiles mediaFile = new MediaFiles();
        mediaFile.setObjectKey(objectKey);
        mediaFile.setBizType(bizType);
        mediaFile.setContentType(initUploadDTO.getContentType());
        mediaFile.setSizeBytes(initUploadDTO.getSizeBytes());
        mediaFile.setSpaceId(initUploadDTO.getSpaceId());
        mediaFile.setCreatedBy(userId);
        mediaFile.setStatus(MediaFilesStatus.UPLOADING);
        mediaFile.setAccessLevel(MediaFilesAccessLevelType.PRIVATE);

        mediaFilesMapper.insert(mediaFile);

        try {
            InitUploadVO vo = toInitVO(mediaFile, isMultiPart);
            log.info("初始化上传成功，fileId: {}, mode: {}, userId: {}", mediaFile.getId(), vo.getMode(), userId);
            return vo;
        } catch (BizException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("初始化上传失败，fileId: {}, objectKey: {}, userId: {}", mediaFile.getId(), mediaFile.getObjectKey(), userId, e);
            throw BizException.of(ErrorCode.INTERNAL_ERROR, "初始化上传失败");
        }

    }

    /**
     * 签发分片
     */
    @Override
    public SignPartsVO signParts(Long fileId, SignPartsDTO signPartsDTO) {
        return null;
    }

    /**
     * 完成上传
     */
    @Override
    public CompleteUploadVO complete(Long fileId, CompleteUploadDTO completeUploadDTO) {
        return null;
    }

    /**
     * 取消上传
     */
    @Override
    public Void abort(Long fileId) {
        return null;
    }

    /**
     * 获取文件内容
     */
    @Override
    public String getContent(Long fileId) {
        return null;
    }

    /**
     * 验证初始化上传DTO
     * 
     * @param initUploadDTO
     */
    private void validateInitUploadDTO(InitUploadDTO initUploadDTO) {
        if (initUploadDTO.getBizType() == null || initUploadDTO.getSizeBytes() <= 0) {
            throw BizException.badRequest("文件大小无效");
        }

        String contentType = initUploadDTO.getContentType().trim().toLowerCase(Locale.ROOT);
        boolean isImage = ossProperties.getAllowedImageTypes().stream().anyMatch(t -> t.equalsIgnoreCase(contentType));
        boolean isVideo = ossProperties.getAllowedVideoTypes().stream().anyMatch(t -> t.equalsIgnoreCase(contentType));

        if (!isImage && !isVideo) {
            throw BizException.badRequest("不支持的文件类型");
        }

        if (isImage && initUploadDTO.getSizeBytes() > ossProperties.getMaxImageBytes()) {
            throw BizException.badRequest("图片大小超出限制");
        }

        if (isVideo && initUploadDTO.getSizeBytes() > ossProperties.getMaxVideoBytes()) {
            throw BizException.badRequest("视频大小超出限制");
        }
    }

    /**
     * 生成对象Key
     * 
     * @param bizType
     * @param contentType
     * @return ossKeyPrefix/bizType/年/月/日/uuid.ext
     * @example cms/article_richtext/2026/08/22/a1b2c3d4e5f6.jpg
     */
    private String generateObjectKey(MediaFilesBizType bizType, String contentType) {
        LocalDate now = LocalDate.now();
        String ext = transformContentType2Extension(contentType);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String prefix = ossProperties.getOssKeyPrefix();

        /** 格式化：ossKeyPrefix/bizType/年/月/日/uuid.ext */
        return String.format("%s/%s/%04d/%02d/%02d/%s%s",
                prefix,
                bizType.getCode(),
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                uuid,
                ext);

    }

    /**
     * 将Content-Type转换为文件扩展名
     * 
     * @param contentType
     * @return
     */
    private String transformContentType2Extension(String contentType) {
        String ext = CONTENT_TYPE_EXTENSION_MAP.get(contentType.toLowerCase(Locale.ROOT));
        if (ext == null) {
            throw BizException.badRequest("不支持的文件类型");
        }

        return ext;
    }

    private InitUploadVO toInitVO(MediaFiles mediaFile, boolean isMultiPart) {
        if (mediaFile == null) {
            throw BizException.badRequest("文件不存在");
        }

        InitUploadVO vo = InitUploadVO.builder()
                .fileId(mediaFile.getId())
                .mode(isMultiPart ? UploadModeType.MULTIPART : UploadModeType.SINGLE)
                .build();

        if (isMultiPart) {
            String uploadId = generateUploadId(mediaFile);
            Long partSize = ossProperties.getMultipartPartSizeBytes();
            int partCount = (int) Math.ceil((double) mediaFile.getSizeBytes() / partSize);

            vo.setUploadId(uploadId);
            vo.setPartSize(partSize);
            vo.setPartCount(partCount);
        } else {
            String putUrl = generatePutUrl(mediaFile);
            InitSingleHeaderVO headers = new InitSingleHeaderVO();
            headers.setContentType(mediaFile.getContentType());

            vo.setPutUrl(putUrl);
            vo.setHeaders(headers);
        }

        return vo;
    }

    /**
     * 生成上传URL
     * 
     * @param mediaFile
     * @return
     */
    private String generatePutUrl(MediaFiles mediaFile) {
        String putUrl = ossStorage.presignPut(mediaFile.getObjectKey(), mediaFile.getContentType(),
                ossProperties.getSignedPutExpireSeconds());

        return putUrl;
    }

    /**
     * 生成上传ID
     * 
     * @param mediaFile
     * @return
     */
    private String generateUploadId(MediaFiles mediaFile) {
        String uploadId = ossStorage.initialMultipart(mediaFile.getObjectKey(), mediaFile.getContentType());

        return uploadId;
    }
}
