package com.lake.knowengine.document.service.impl;

import com.lake.knowengine.document.service.VectorStoreService;
import com.lake.knowengine.rag.constant.MetadataKeyConstant;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * 向量存储服务实现
 * 统一负责文本嵌入、向量写入、向量删除等与 Elasticsearch 向量库的交互。
 */
@Slf4j
@Service
public class VectorStoreServiceImpl implements VectorStoreService {

    @Autowired
    private ElasticsearchEmbeddingStore embeddingStore;

    @Override
    public void removeByDocId(Long docId) {
        try {
            Filter filter = metadataKey(MetadataKeyConstant.DOC_ID).isEqualTo(docId);
            embeddingStore.removeAll(filter);
            log.info("按docId删除向量成功, docId: {}", docId);
        } catch (Exception e) {
            log.warn("按docId删除向量失败, docId: {}, error: {}", docId, e.getMessage());
        }
    }
}
