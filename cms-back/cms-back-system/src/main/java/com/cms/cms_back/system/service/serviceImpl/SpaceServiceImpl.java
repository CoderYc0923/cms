package com.cms.cms_back.system.service.serviceImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cms.cms_back.common.exception.BizException;
import com.cms.cms_back.common.exception.ErrorCode;
import com.cms.cms_back.pojo.entity.Node;
import com.cms.cms_back.pojo.entity.Space;
import com.cms.cms_back.pojo.enums.NodeStatus;
import com.cms.cms_back.pojo.vo.space.SpaceNodeTreeVO;
import com.cms.cms_back.system.mapper.NodeMapper;
import com.cms.cms_back.system.mapper.SpaceMapper;
import com.cms.cms_back.system.service.SpaceService;

@Service
public class SpaceServiceImpl implements SpaceService {

    private final SpaceMapper spaceMapper;
    private final NodeMapper nodeMapper;

    public SpaceServiceImpl(SpaceMapper spaceMapper, NodeMapper nodeMapper) {
        this.spaceMapper = spaceMapper;
        this.nodeMapper = nodeMapper;
    }

    @Override
    public List<SpaceNodeTreeVO> getTree(String slug) {

        // 找到slug对应所有的node
        List<Node> nodeList = getNodesBySlug(slug);

        // 将node转化成树形
        List<SpaceNodeTreeVO> tree = transferList2Tree(nodeList);

        return tree;
    }

    /**
     * 将nodeList转化成树形
     * 
     * @param nodeList
     * @return
     */
    private List<SpaceNodeTreeVO> transferList2Tree(List<Node> nodeList) {
        if (nodeList == null || nodeList.isEmpty()) {
            return new ArrayList<>();
        }

        // 将每个node转化成SpaceNodeTreeVO
        Map<Long, SpaceNodeTreeVO> map = new HashMap<>();
        for (Node node : nodeList) {
            map.put(node.getId(), toVO(node));
        }

        // 将SpaceNodeTreeVO转换成树形
        List<SpaceNodeTreeVO> tree = new ArrayList<>();
        for (Node node : nodeList) {
            SpaceNodeTreeVO vo = map.get(node.getId());
            Long parentId = node.getParentId();

            if (parentId == null || parentId <= 0) {
                tree.add(vo);
                continue;
            }

            SpaceNodeTreeVO parent = map.get(parentId);
            if (parent != null) {
                parent.getChildren().add(vo);
            } else {
                tree.add(vo);
            }
        }

        return tree;
    }

    /**
     * 将node转化成SpaceNodeTreeVO
     * 
     * @param node
     * @return
     */
    private SpaceNodeTreeVO toVO(Node node) {
        if (node == null) {
            return null;
        }

        SpaceNodeTreeVO vo = SpaceNodeTreeVO.builder()
                .id(node.getId())
                .title(node.getTitle())
                .type(node.getType().getCode())
                .sort(node.getSort())
                .children(new ArrayList<>())
                .build();

        return vo;
    }

    /**
     * 找到slug对应所有的node
     * 
     * @param slug
     * @return
     */
    private List<Node> getNodesBySlug(String slug) {
        Space space = getSpaceBySlug(slug);
        if (space == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "空间不存在");
        }

        return nodeMapper.selectList(
                new LambdaQueryWrapper<Node>()
                        .eq(Node::getSpaceId, space.getId())
                        .isNull(Node::getDeletedAt)
                        .orderByAsc(Node::getSort, Node::getCreatedAt));

    }

    /**
     * 根据slug找到空间
     * 
     * @param slug
     * @return
     */
    private Space getSpaceBySlug(String slug) {
        if (!StringUtils.hasText(slug)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "空间不存在");
        }

        return spaceMapper.selectOne(
                new LambdaQueryWrapper<Space>()
                        .eq(Space::getSlug, slug));
    }

}
