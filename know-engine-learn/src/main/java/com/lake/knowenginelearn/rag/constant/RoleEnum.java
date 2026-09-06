package com.lake.knowenginelearn.rag.constant;

import com.lake.knowenginelearn.chat.constant.ChatSource;

/**
 * 角色
 *
 * @author Hollis
 */
public enum RoleEnum {

    /**
     * 已购车用户
     */
    OWNER,

    /**
     * 未购车用户
     */
    VISITOR,

    /**
     * 客服
     */
    CUSTOMER_SERVICE;

    public static RoleEnum getRoleEnum(ChatSource chatSource, Boolean hasOrder) {
        if (chatSource == ChatSource.STAFF_DING) {
            return CUSTOMER_SERVICE;
        }

        if (hasOrder) {
            return OWNER;
        }

        return VISITOR;
    }
}
