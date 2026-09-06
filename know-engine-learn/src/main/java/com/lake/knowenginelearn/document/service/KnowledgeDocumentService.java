package com.lake.knowenginelearn.document.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.lake.knowenginelearn.document.entity.KnowledgeDocument;

import java.util.List;

/**
 * 知识文档表 Service 接口
 */
public interface KnowledgeDocumentService extends IService<KnowledgeDocument> {

    /**
     * 删除文档，并级联逻辑删除该文档下的所有分段
     *
     * @param docId 文档ID
     * @return 是否删除成功
     */
    boolean removeDocumentWithSegments(Long docId);

    /**
     * 批量删除文档，并级联逻辑删除这些文档下的所有分段
     *
     * @param docIds 文档ID列表
     * @return 是否删除成功
     */
    boolean removeDocumentsWithSegments(List<Long> docIds);

    /**
     * 让指定版本失效：
     * 1. 清理该版本在 ES 中的向量数据
     * 2. 将该版本下所有分段状态从 VECTOR_STORED 降为 STORED，并清空 embeddingId
     * 3. 将版本记录状态从 VECTOR_STORED 降为 CHUNKED
     *
     * @param versionId 版本ID（knowledge_document_version.version_id）
     */
    void deactivateVersion(Long versionId);

    /**
     * 让指定版本生效（重新向量化）：
     * 1. 校验版本状态必须为 CHUNKED
     * 2. 对该版本下所有 STORED 且未向量化的分段重新 embed 并写入 ES
     * 3. 将分段状态更新为 VECTOR_STORED
     * 4. 将版本记录状态从 CHUNKED 升为 VECTOR_STORED
     *
     * @param versionId 版本ID（knowledge_document_version.version_id）
     */
    void activateVersion(Long versionId);

    /**
     * 扫描需要清理的文档
     *
     * @return 需要清理的文档列表
     */
    List<KnowledgeDocument> scanDocumentsNeedingCleanup();
}
