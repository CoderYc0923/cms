package com.cms.cms_back.system.service.serviceImpl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.common.exception.ErrorCode;
import com.cms.cms_back.pojo.dto.article.CreateArticleDTO;
import com.cms.cms_back.pojo.dto.node.CreateNodeDTO;
import com.cms.cms_back.pojo.dto.node.UpdateNodeDTO;
import com.cms.cms_back.pojo.entity.Node;
import com.cms.cms_back.pojo.entity.Space;
import com.cms.cms_back.pojo.enums.NodeStatus;
import com.cms.cms_back.pojo.enums.NodeType;
import com.cms.cms_back.system.mapper.NodeMapper;
import com.cms.cms_back.system.mapper.SpaceMapper;
import com.cms.cms_back.system.service.ArticleService;
import com.cms.cms_back.system.service.NodeService;

@Service
public class NodeServiceImpl implements NodeService {

    private final NodeMapper nodeMapper;
    private final SpaceMapper spaceMapper;
    private final ArticleService articleService;

    public NodeServiceImpl(NodeMapper nodeMapper, SpaceMapper spaceMapper, ArticleService articleService) {
        this.spaceMapper = spaceMapper;
        this.nodeMapper = nodeMapper;
        this.articleService = articleService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void create(CreateNodeDTO dto, Long userId) {
        Node node = new Node();
        node.setType(NodeType.fromCode(dto.getType()));
        node.setTitle(dto.getTitle());

        long spaceId = getSpaceSlugId(dto.getSlug());
        node.setSpaceId(spaceId);

        if (dto.getParentId() != null && dto.getParentId() > 0) {
            Node parentNode = getNodeById(dto.getParentId());
            if (parentNode == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "父节点不存在");
            }
            if (parentNode.getSpaceId() != spaceId) {
                throw new BizException(ErrorCode.BAD_REQUEST, "父节点与空间不匹配");
            }

            node.setParentId(dto.getParentId());
        }

        if (dto.getSort() != null && dto.getSort() >= 0) {
            node.setSort(dto.getSort());
        }

        node.setStatus(NodeStatus.VISIBLE);

        nodeMapper.insert(node);

        if (NodeType.fromCode(dto.getType()) == NodeType.ARTICLE) {
            createDraftArticle(node.getId(), userId);
        }
    }

    @Override
    public void update(Long id, UpdateNodeDTO dto) {
    }

    @Override
    public void delete(Long id) {
    }

    /**
     * 创建草稿文章
     * @param nodeId
     * @param userId
     */
    private void createDraftArticle(Long nodeId, Long userId) {
        CreateArticleDTO articleDTO = new CreateArticleDTO();
        articleDTO.setNodeId(nodeId);

        articleService.create(articleDTO, userId);
    }

    /**
     * 获取节点
     * 
     * @param id
     * @return
     */
    private Node getNodeById(Long id) {
        return nodeMapper.selectOne(
                new LambdaQueryWrapper<Node>()
                        .eq(Node::getId, id)
                        .isNull(Node::getDeletedAt));
    }

    /**
     * 获取空间ID
     * 
     * @param slug
     * @return
     */
    private Long getSpaceSlugId(String slug) {
        Space space = spaceMapper.selectOne(
                new LambdaQueryWrapper<Space>()
                        .eq(Space::getSlug, slug));

        if (space == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "空间不存在");
        }

        return space.getId();
    }
}
