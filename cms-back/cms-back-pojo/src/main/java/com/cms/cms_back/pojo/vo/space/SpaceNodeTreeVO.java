package com.cms.cms_back.pojo.vo.space;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpaceNodeTreeVO {

    private Long id;

    private String title;

    private String type;

    private Integer sort;

    private List<SpaceNodeTreeVO> children;
}
