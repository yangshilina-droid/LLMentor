package com.lake.knowenginelearn.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lake.knowenginelearn.business.entity.MyCar;
import org.apache.ibatis.annotations.Mapper;

/**
 * 我的车辆信息 Mapper 接口
 */
@Mapper
public interface MyCarMapper extends BaseMapper<MyCar> {
}
