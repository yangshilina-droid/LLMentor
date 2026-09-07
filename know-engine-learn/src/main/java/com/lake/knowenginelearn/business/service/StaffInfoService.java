package com.lake.knowenginelearn.business.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lake.knowenginelearn.business.entity.StaffInfo;

/**
 * 员工信息表 Service 接口
 */
public interface StaffInfoService extends IService<StaffInfo> {

    /**
     * 根据工号查询员工信息
     *
     * @param empId 工号
     * @return 员工信息，不存在返回 null
     */
    StaffInfo getByEmpId(String empId);
}
