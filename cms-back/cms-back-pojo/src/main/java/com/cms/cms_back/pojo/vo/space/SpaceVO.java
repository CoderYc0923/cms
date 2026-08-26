package com.cms.cms_back.pojo.vo.space;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpaceVO {

    private Long id;

    private String name;

    private String slug;

    private String description;

    private Integer sort;

    private Integer status;
}
