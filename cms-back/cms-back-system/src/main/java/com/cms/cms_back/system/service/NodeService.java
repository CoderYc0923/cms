package com.cms.cms_back.system.service;

import com.cms.cms_back.pojo.dto.node.CreateNodeDTO;
import com.cms.cms_back.pojo.dto.node.UpdateNodeDTO;

public interface NodeService {

    void create(CreateNodeDTO dto);

    void update(Long id, UpdateNodeDTO dto);

    void delete(Long id);
}
