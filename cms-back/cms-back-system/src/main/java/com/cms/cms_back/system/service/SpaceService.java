package com.cms.cms_back.system.service;

import java.util.List;

import com.cms.cms_back.pojo.vo.space.SpaceNodeTreeVO;

public interface SpaceService {

    List<SpaceNodeTreeVO> getTree(String slug);
}
