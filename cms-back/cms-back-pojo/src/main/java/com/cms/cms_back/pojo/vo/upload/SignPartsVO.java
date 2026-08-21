package com.cms.cms_back.pojo.vo.upload;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 签发分片列表VO
 * SignPartsVO
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignPartsVO {

    /**
     * 签发分片列表
     */
    private List<SignPartVO> parts;
}
