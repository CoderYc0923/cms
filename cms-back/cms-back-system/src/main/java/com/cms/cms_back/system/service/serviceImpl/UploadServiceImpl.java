package com.cms.cms_back.system.service.serviceImpl;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cms.cms_back.system.mapper.MediaFilesMapper;
import com.cms.cms_back.system.oss.OssObjectMeta;
import com.cms.cms_back.system.oss.OssProperties;
import com.cms.cms_back.system.oss.OssStorage;
import com.cms.cms_back.system.oss.OssUploadPart;
import com.cms.cms_back.system.service.UploadService;
import com.cms.cms_back.pojo.vo.upload.InitUploadVO;
import com.cms.cms_back.pojo.vo.upload.SignPartVO;
import com.cms.cms_back.pojo.dto.upload.InitUploadDTO;
import com.cms.cms_back.pojo.vo.upload.SignPartsVO;
import com.cms.cms_back.pojo.dto.upload.SignPartsDTO;
import com.cms.cms_back.pojo.dto.upload.CompleteUploadDTO.CompletePartDTO;
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
    private static final String OSS_STABLE_URL_TEMPLATE = "/api/public/files/%s/content";
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
        mediaFile.setOriginalName(initUploadDTO.getFileName());
        mediaFile.setObjectKey(objectKey);
        mediaFile.setBizType(bizType);
        mediaFile.setContentType(initUploadDTO.getContentType());
        mediaFile.setSizeBytes(initUploadDTO.getSizeBytes());
        mediaFile.setSpaceId(initUploadDTO.getSpaceId());
        mediaFile.setCreatedBy(userId);
        mediaFile.setStatus(MediaFilesStatus.UPLOADING);
        mediaFile.setAccessLevel(MediaFilesAccessLevelType.PRIVATE);

        try {
            InitUploadVO vo = toInitVO(mediaFile, isMultiPart);

            log.info("初始化上传成功，fileId: {}, mode: {}, userId: {}", mediaFile.getId(), vo.getMode(), userId);
            return vo;
        } catch (BizException e) {
            cleanupInitOnOss(objectKey, mediaFile.getUploadId());
            throw e;
        } catch (Exception e) {
            cleanupInitOnOss(objectKey, mediaFile.getUploadId());
            log.error("初始化上传失败，fileId: {}, objectKey: {}, userId: {}", mediaFile.getId(), mediaFile.getObjectKey(),
                    userId, e);
            throw BizException.of(ErrorCode.INTERNAL_ERROR, "初始化上传失败");
        }

    }

    /**
     * 签发分片
     */
    @Override
    public SignPartsVO signParts(Long fileId, SignPartsDTO signPartsDTO, Long userId) {
        MediaFiles mediaFiles = validateCompleteUploadFile(fileId, userId);
        if (mediaFiles.getStatus() == MediaFilesStatus.READY) {
            throw BizException.badRequest("文件已上传完成，请勿重复签发分片");
        }

        if (!isMultiPartUpload(mediaFiles)) {
            throw BizException.badRequest("文件不是分片上传，请勿签发分片");
        }

        if (signPartsDTO == null || signPartsDTO.getPartNumbers() == null || signPartsDTO.getPartNumbers().isEmpty()) {
            throw BizException.badRequest("分片号不能为空");
        }

        long partSize = ossProperties.getMultipartPartSizeBytes();
        int partCount = (int) Math.ceil((double) mediaFiles.getSizeBytes() / partSize);

        Set<Integer> seen = new HashSet<>();
        for (Integer partNumber : signPartsDTO.getPartNumbers()) {
            if (partNumber == null || partNumber < 1 || partNumber > partCount) {
                throw BizException.badRequest("分片号无效");
            }
            if (seen.contains(partNumber)) {
                throw BizException.badRequest("分片号重复");
            }
            seen.add(partNumber);
        }

        SignPartsVO vo = toSignPartsVO(mediaFiles, signPartsDTO);

        return vo;
    }

    /**
     * 完成上传
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public CompleteUploadVO complete(Long fileId, CompleteUploadDTO completeUploadDTO, Long userId) {
        MediaFiles mediaFiles = validateCompleteUploadFile(fileId, userId);

        if (mediaFiles.getStatus() == MediaFilesStatus.READY) {
            return toCompleteVO(mediaFiles);
        }

        boolean isMultiPart = isMultiPartUpload(mediaFiles);

        try {
            if (isMultiPart) {
                completeMultiPartUpload(mediaFiles, completeUploadDTO);
            } else {
                completeSingleUpload(mediaFiles);
            }

            mediaFiles.setStatus(MediaFilesStatus.READY);
            mediaFiles.setUploadId(null);
            mediaFilesMapper.updateById(mediaFiles);

            log.info("完成上传操作成功，fileId: {}, userId: {}, objectKey: {}, mode: {}", fileId, userId,
                    mediaFiles.getObjectKey(), isMultiPart ? UploadModeType.MULTIPART : UploadModeType.SINGLE);
            return toCompleteVO(mediaFiles);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("完成上传操作失败，fileId: {}, userId: {}, objectKey: {}", fileId, userId, mediaFiles.getObjectKey(), e);
            throw BizException.of(ErrorCode.INTERNAL_ERROR, "完成上传操作失败");
        }

    }

    /**
     * 取消上传
     */
    @Override
    public void abort(Long fileId, Long userId) {
        MediaFiles mediaFiles = validateCompleteUploadFile(fileId, userId);
        if (mediaFiles.getStatus() == MediaFilesStatus.READY) {
            throw BizException.badRequest("文件已上传完成，请勿重复取消上传");
        }

        abortUpload(mediaFiles, userId);
    }

    /**
     * 获取文件内容
     */
    @Override
    public String getContent(Long fileId, Long userId, boolean isPublic) {
        if (!isPublic && userId == null) {
            throw BizException.badRequest("用户ID不能为空");
        }

        MediaFiles mediaFiles = validateReadyUploadFile(fileId, isPublic);
        return generatePresignContentUrl(mediaFiles);
    }

    /**
     * 取消上传
     * 
     * @param mediaFiles
     * @param userId
     */
    private void abortUpload(MediaFiles mediaFiles, Long userId) {
        try {
            if (isMultiPartUpload(mediaFiles)) {
                /** 分片上传已经上传的部分需要取消 */
                ossStorage.abortMultipart(mediaFiles.getObjectKey(), mediaFiles.getUploadId());
            } else {
                /** 单文件上传取消时可能已经put到OSS了，尝试删除一下 */
                ossStorage.deleteObject(mediaFiles.getObjectKey());
            }

            mediaFiles.setStatus(MediaFilesStatus.FAILED);
            mediaFiles.setUploadId(null);
            mediaFiles.setEtag(null);

            mediaFilesMapper.updateById(mediaFiles);
            log.info("取消上传成功，fileId: {}, userId: {}, objectKey: {}", mediaFiles.getId(), userId,
                    mediaFiles.getObjectKey());
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("取消上传失败，fileId: {}, userId: {}, objectKey: {}", mediaFiles.getId(), userId,
                    mediaFiles.getObjectKey(), e);
            throw BizException.of(ErrorCode.INTERNAL_ERROR, "取消上传失败");
        }
    }

    /**
     * 完成分片上传
     * 
     * @param mediaFiles
     * @param completeUploadDTO
     */
    private void completeMultiPartUpload(MediaFiles mediaFiles, CompleteUploadDTO completeUploadDTO) {
        if (completeUploadDTO == null || completeUploadDTO.getParts() == null
                || completeUploadDTO.getParts().isEmpty()) {
            throw BizException.badRequest("分片信息不能为空");
        }

        List<CompletePartDTO> parts = completeUploadDTO.getParts();
        validateCompleteParts(mediaFiles, parts);

        List<OssUploadPart> ossParts = parts.stream()
                .map(p -> new OssUploadPart(p.getPartNumber(), p.getEtag()))
                .collect(Collectors.toList());

        String etag = ossStorage.completeMultipart(mediaFiles.getObjectKey(), mediaFiles.getUploadId(), ossParts);

        mediaFiles.setEtag(etag);
    }

    /**
     * 完成单次上传
     * 
     * @param mediaFiles
     */
    private void completeSingleUpload(MediaFiles mediaFiles) {
        OssObjectMeta metaDMeta = ossStorage.head(mediaFiles.getObjectKey());
        mediaFiles.setEtag(metaDMeta.etag());
        mediaFiles.setSizeBytes(metaDMeta.sizeBytes());
    }

    /**
     * 验证上传文件是否准备就绪
     * 
     * @param fileId
     * @param isPublic
     * @return
     */
    private MediaFiles validateReadyUploadFile(Long fileId, boolean isPublic) {
        if (fileId == null || fileId <= 0) {
            throw BizException.badRequest("文件ID无效");
        }

        MediaFiles mediaFile = mediaFilesMapper.selectById(fileId);
        if (mediaFile == null) {
            throw BizException.notFound("文件不存在");
        }
        if (mediaFile.getStatus() != MediaFilesStatus.READY) {
            throw BizException.notFound("文件不存在");
        }
        if (isPublic && mediaFile.getAccessLevel() != MediaFilesAccessLevelType.PUBLIC) {
            throw BizException.badRequest("文件不存在");
        }

        return mediaFile;
    }

    /**
     * 验证完成上传分片
     * 
     * @param parts
     */
    private void validateCompleteParts(MediaFiles mediaFiles, List<CompletePartDTO> parts) {
        long partSize = ossProperties.getMultipartPartSizeBytes();
        /** 计算期望的分片数量 */
        int expectedPartCount = (int) Math.ceil((double) mediaFiles.getSizeBytes() / partSize);

        if (parts.size() != expectedPartCount) {
            throw BizException.badRequest("分片数量不匹配");
        }

        Set<Integer> partNumbers = new HashSet<>();
        for (CompletePartDTO part : parts) {
            int partNumber = part.getPartNumber();
            if (partNumber < 1 || partNumber > expectedPartCount) {
                throw BizException.badRequest("分片号无效");
            }
            if (partNumbers.contains(partNumber)) {
                throw BizException.badRequest("分片号重复");
            }
            partNumbers.add(partNumber);
        }

    }

    /**
     * 验证完成上传文件
     * 
     * @param fileId
     * @param userId
     * @return
     */
    private MediaFiles validateCompleteUploadFile(Long fileId, Long userId) {
        if (fileId == null || fileId <= 0) {
            throw BizException.badRequest("文件ID无效");
        }

        MediaFiles mediaFile = mediaFilesMapper.selectById(fileId);
        if (mediaFile == null) {
            throw BizException.notFound("文件不存在");
        }
        if (!mediaFile.getCreatedBy().equals(userId)) {
            throw BizException.forbidden("无权限操作此文件");
        }
        if (mediaFile.getStatus() != MediaFilesStatus.UPLOADING && mediaFile.getStatus() != MediaFilesStatus.READY) {
            throw BizException.badRequest("文件状态异常");
        }

        return mediaFile;
    }

    /**
     * 判断是否是分片上传
     * 
     * @param mediaFile
     * @return
     */
    private boolean isMultiPartUpload(MediaFiles mediaFile) {
        return mediaFile.getUploadId() != null && !mediaFile.getUploadId().isBlank();
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

    /**
     * 生成预签名获取URL
     * 
     * @param mediaFiles
     * @return
     */
    private String generatePresignContentUrl(MediaFiles mediaFiles) {
        try {
            String url = ossStorage.presignGet(mediaFiles.getObjectKey(), ossProperties.getSignedGetExpireSeconds());
            return url;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成预签名获取URL失败，fileId: {}, userId: {}, objectKey: {}", mediaFiles.getId(),
                    mediaFiles.getCreatedBy(), mediaFiles.getObjectKey(), e);
            throw BizException.of(ErrorCode.INTERNAL_ERROR, "生成预签名获取URL失败");
        }
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

    /**
     * 生成稳定URL
     * 
     * @param fileId
     * @return
     */
    private String generateStableUrl(Long fileId) {
        return String.format(OSS_STABLE_URL_TEMPLATE, fileId);
    }

    /**
     * 初始化上传失败清理已上传的部分
     * @param objectKey
     * @param uploadId
     */
    private void cleanupInitOnOss(String objectKey, String uploadId) {
        if (uploadId == null)
            return;

        try {
            ossStorage.abortMultipart(objectKey, uploadId);
        } catch (Exception e) {
            log.warn("init 失败补偿 abortMultipart 失败，objectKey: {}, uploadId: {}", objectKey, uploadId, e);
        }
    }

    private InitUploadVO toInitVO(MediaFiles mediaFile, boolean isMultiPart) {
        if (mediaFile == null) {
            throw BizException.badRequest("文件不存在");
        }

        InitUploadVO vo = InitUploadVO.builder()
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

        mediaFile.setUploadId(vo.getUploadId());
        mediaFilesMapper.insert(mediaFile);
        vo.setFileId(mediaFile.getId());

        return vo;
    }

    private CompleteUploadVO toCompleteVO(MediaFiles mediaFiles) {
        return CompleteUploadVO.builder()
                .stableUrl(generateStableUrl(mediaFiles.getId()))
                .build();
    }

    private SignPartsVO toSignPartsVO(MediaFiles mediaFiles, SignPartsDTO signPartsDTO) {
        List<SignPartVO> signdParts = signPartsDTO.getPartNumbers().stream()
                .map(p -> {
                    String putUrl = ossStorage.presignUploadPart(mediaFiles.getObjectKey(), mediaFiles.getUploadId(), p,
                            ossProperties.getSignedPutExpireSeconds());
                    SignPartVO vo = new SignPartVO();
                    vo.setPartNumber(p);
                    vo.setPutUrl(putUrl);
                    return vo;
                })
                .collect(Collectors.toList());

        return SignPartsVO.builder().parts(signdParts).build();
    }
}
