package com.lake.knowenginelearn.business.service.impl;

import com.lake.knowenginelearn.business.constant.StaffStatus;
import com.lake.knowenginelearn.business.entity.MyCar;
import com.lake.knowenginelearn.business.entity.StaffInfo;
import com.lake.knowenginelearn.business.service.MyCarService;
import com.lake.knowenginelearn.business.service.StaffInfoService;
import com.lake.knowenginelearn.business.service.UserRoleService;
import com.lake.knowenginelearn.chat.entity.ChatParam;
import com.lake.knowenginelearn.rag.constant.RoleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserRoleServiceImpl implements UserRoleService {

    @Autowired
    private StaffInfoService staffInfoService;

    @Autowired
    private MyCarService myCarService;

    @Override
    public RoleEnum getUserRole(ChatParam chatParam) {
        //再次查询一下车辆，避免水平权限漏洞
        MyCar myCar = myCarService.getCarByUser(chatParam.intentRecognitionResult().entities().car_id(), chatParam.userId());
        if (myCar != null) {
            return RoleEnum.OWNER;
        }

        StaffInfo staffInfo = staffInfoService.getById(chatParam.userId());
        if (staffInfo != null && staffInfo.getStatus() == StaffStatus.ON_JOB) {
            return RoleEnum.CUSTOMER_SERVICE;
        }

        return RoleEnum.VISITOR;
    }
}
