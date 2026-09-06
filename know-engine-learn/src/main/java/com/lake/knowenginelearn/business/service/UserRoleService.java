package com.lake.knowenginelearn.business.service;

import com.lake.knowenginelearn.chat.entity.ChatParam;
import com.lake.knowenginelearn.rag.constant.RoleEnum;

public interface UserRoleService {

    public RoleEnum getUserRole(ChatParam chatParam);
}
