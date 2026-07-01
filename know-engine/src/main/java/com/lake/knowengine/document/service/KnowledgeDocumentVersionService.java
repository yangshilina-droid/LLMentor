package com.lake.knowengine.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lake.knowengine.document.constant.DocumentStatus;
import com.lake.knowengine.document.entity.KnowledgeDocumentVersion;

public interface KnowledgeDocumentVersionService extends IService<KnowledgeDocumentVersion> {

    /**
     * 检查内容哈希是否已存在
     *
     * @param contentHash 内容哈希值
     * @return 是否已存在
     */
    boolean existsByContentHash(String contentHash);

    /**
     * 创建文档版本记录
     *
     * @param docId 关联文档ID
     * @param version 版本号
     * @param docUrl 原始文档URL
     * @param convertedDocUrl 转换后文档URL
     * @param uploadUser 上传用户
     * @param contentHash 内容哈希值
     * @param status 版本状态
     * @param changelog 版本变更说明
     * @return 文档版本记录
     */
    KnowledgeDocumentVersion createVersionRecord(Long docId,
                                                 String version,
                                                 String docUrl,
                                                 String convertedDocUrl,
                                                 String uploadUser,
                                                 String contentHash,
                                                 DocumentStatus status,
                                                 String changelog);
}
