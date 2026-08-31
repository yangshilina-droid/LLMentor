package com.lake.knowenginelearn.business.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lake.knowenginelearn.business.entity.CarOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 车辆订单 Mapper 接口
 */
@Mapper
public interface CarOrderMapper extends BaseMapper<CarOrder> {
}
