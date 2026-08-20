package com.cms.cms_back.system.service.serviceImpl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.common.exception.ErrorCode;
import com.cms.cms_back.pojo.dto.article.CreateArticleDTO;
import com.cms.cms_back.pojo.dto.mq.ArticleNodeMessage;
import com.cms.cms_back.pojo.dto.node.CreateNodeDTO;
import com.cms.cms_back.pojo.dto.node.UpdateNodeDTO;
import com.cms.cms_back.pojo.entity.Article;
import com.cms.cms_back.pojo.entity.Node;
import com.cms.cms_back.pojo.entity.Space;
import com.cms.cms_back.pojo.enums.NodeStatus;
import com.cms.cms_back.pojo.enums.NodeType;
import com.cms.cms_back.system.mapper.NodeMapper;
import com.cms.cms_back.system.mapper.SpaceMapper;
import com.cms.cms_back.system.mq.producers.CreateArticleNodeProducer;
import com.cms.cms_back.system.service.ArticleService;
import com.cms.cms_back.system.service.NodeService;

@Service
public class NodeServiceImpl implements NodeService {

    private final NodeMapper nodeMapper;
    private final SpaceMapper spaceMapper;
    private final ArticleService articleService;
    private final CreateArticleNodeProducer createArticleNodeProducer;

    public NodeServiceImpl(NodeMapper nodeMapper, SpaceMapper spaceMapper, ArticleService articleService, CreateArticleNodeProducer createArticleNodeProducer) {
        this.spaceMapper = spaceMapper;
        this.nodeMapper = nodeMapper;
        this.articleService = articleService;
        this.createArticleNodeProducer = createArticleNodeProducer;
    }

    @Override
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
            ArticleNodeMessage message = new ArticleNodeMessage();
            message.setUserId(userId);
            message.setNodeId(node.getId());
            message.setTitle(node.getTitle());

            createArticleNodeProducer.publish(message);
        }
    }

    @Override
    public void update(Long id, UpdateNodeDTO dto) {
        Node node = getNodeById(id);
        if (node == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "节点不存在");
        }

        if (dto.getSort() == null || dto.getSort() < 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, "排序不能为空");
        }

        LambdaUpdateWrapper<Node> updateWrapper = new LambdaUpdateWrapper<>();

        updateWrapper
            .eq(Node::getId, id)
            .isNull(Node::getDeletedAt)
            .set(Node::getTitle, dto.getTitle())
            .set(Node::getSort, dto.getSort());


        nodeMapper.update(null, updateWrapper);
    }

    /**
     * 删除节点（软删除）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Node node = getNodeById(id);
        if (node == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "节点不存在");
        }

        if (node.getType() == NodeType.GROUP || node.getType() == NodeType.MENU) {

            long count = nodeMapper.selectCount(
                new LambdaQueryWrapper<Node>()
                    .eq(Node::getParentId, id)
                    .isNull(Node::getDeletedAt)
            );

            if (count > 0) {
                throw new BizException(ErrorCode.CONFLICT, "节点下有子节点，不能删除");
            }
        }

        if (node.getType() == NodeType.ARTICLE) {
            deleteArticleByNodeId(node.getId());
        }

        nodeMapper.update(null, new LambdaUpdateWrapper<Node>()
            .eq(Node::getId, id)
            .isNull(Node::getDeletedAt)
            .set(Node::getDeletedAt, LocalDateTime.now())
        );
    }
    
    /**
     * 删除文章
     * @param nodeId
     */
    private void deleteArticleByNodeId(Long nodeId) {
        articleService.delete(nodeId);
    }

    /**
     * 获取节点
     * 
     * @param id
     * @return
     */
    private Node getNodeById(Long id) {
        if (id == null || id <= 0) {
            return null;
        }
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
