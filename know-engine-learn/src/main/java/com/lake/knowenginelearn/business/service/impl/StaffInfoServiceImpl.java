package com.lake.knowenginelearn.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowenginelearn.business.entity.StaffInfo;
import com.lake.knowenginelearn.business.mapper.StaffInfoMapper;
import com.lake.knowenginelearn.business.service.StaffInfoService;
import org.springframework.stereotype.Service;

/**
 * 员工信息表 Service 实现类
 */
@Service
public class StaffInfoServiceImpl extends ServiceImpl<StaffInfoMapper, StaffInfo> implements StaffInfoService {
}
