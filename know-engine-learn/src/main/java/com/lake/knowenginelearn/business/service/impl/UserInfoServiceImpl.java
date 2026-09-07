package com.lake.knowenginelearn.business.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowenginelearn.business.entity.UserInfo;
import com.lake.knowenginelearn.business.mapper.UserInfoMapper;
import com.lake.knowenginelearn.business.service.UserInfoService;
import org.springframework.stereotype.Service;

/**
 * 客户信息表 Service 实现类
 */
@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {
}
