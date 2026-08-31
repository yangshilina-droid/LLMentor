package com.lake.knowenginelearn.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lake.knowenginelearn.business.entity.CarInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车型信息 Mapper 接口
 */
@Mapper
public interface CarInfoMapper extends BaseMapper<CarInfo> {
}

