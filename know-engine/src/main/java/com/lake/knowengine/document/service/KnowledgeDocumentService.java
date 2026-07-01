package com.lake.knowengine.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lake.knowengine.document.entity.KnowledgeDocument;

public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    /**
     * 删除文档，并级联逻辑删除该文档下的所有分段
     *
     * @param docId 文档ID
     * @return 是否删除成功
     */
    boolean removeDocumentWithSegments(Long docId);

}
