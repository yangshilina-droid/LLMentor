package com.lake.knowenginelearn.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lake.knowenginelearn.business.entity.UserInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户信息表 Mapper
 */
@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {
}
