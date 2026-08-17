package com.cms.cms_back.system.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cms.cms_back.pojo.entity.User;

/**
 * 用户Mapper
 * @author Cyrus
 * @date 2026-08-17
 * UserMapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
