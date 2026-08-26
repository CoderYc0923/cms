package com.cms.cms_back.system.service;

import com.cms.cms_back.pojo.dto.node.CreateNodeDTO;
import com.cms.cms_back.pojo.dto.node.UpdateNodeDTO;
import com.cms.cms_back.pojo.entity.Node;
import com.cms.cms_back.pojo.enums.NodeStatus;

public interface NodeService {

    void create(CreateNodeDTO dto, Long userId);

    void update(Long id, UpdateNodeDTO dto);

    void delete(Long id);
}
