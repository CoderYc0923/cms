package com.cms.cms_back.system.service;

import java.util.List;

import com.cms.cms_back.pojo.vo.space.SpaceVO;
import com.cms.cms_back.pojo.dto.space.CreateSpaceDTO;
import com.cms.cms_back.pojo.dto.space.UpdateSpaceDTO;
import com.cms.cms_back.pojo.vo.space.SpaceNodeTreeVO;

public interface SpaceService {

    List<SpaceNodeTreeVO> getTree(String slug);

    List<SpaceNodeTreeVO> getPublicTree(String slug);

    List<SpaceVO> getList(Integer status);

    void create(CreateSpaceDTO dto);

    void update(Long id, UpdateSpaceDTO dto);
}
