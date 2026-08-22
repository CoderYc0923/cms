package com.cms.cms_back.system.service;

import com.cms.cms_back.pojo.dto.upload.InitUploadDTO;
import com.cms.cms_back.pojo.vo.upload.InitUploadVO;
import com.cms.cms_back.pojo.vo.upload.SignPartsVO;
import com.cms.cms_back.pojo.dto.upload.SignPartsDTO;
import com.cms.cms_back.pojo.dto.upload.CompleteUploadDTO;
import com.cms.cms_back.pojo.vo.upload.CompleteUploadVO;

public interface UploadService {

    InitUploadVO init(InitUploadDTO initUploadDTO, Long userId);

    SignPartsVO signParts(Long fileId, SignPartsDTO signPartsDTO);

    CompleteUploadVO complete(Long fileId, CompleteUploadDTO completeUploadDTO);

    Void abort(Long fileId);

    String getContent(Long fileId);
}
