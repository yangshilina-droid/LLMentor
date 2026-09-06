package com.lake.knowenginelearn.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lake.knowenginelearn.business.entity.StaffInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 员工信息表 Mapper
 */
@Mapper
public interface StaffInfoMapper extends BaseMapper<StaffInfo> {
}
