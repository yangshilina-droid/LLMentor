package com.lake.knowengine.document.service;

public interface VectorStoreService {

    /**
     * 按文档ID删除向量数据
     *
     * @param docId 文档ID
     */
    void removeByDocId(Long docId);
}
