package com.lake.knowenginelearn.document.event;

import com.lake.knowenginelearn.document.entity.KnowledgeDocument;
import org.springframework.context.ApplicationEvent;

/**
 * 文档已转换事件
 * 当文档状态变更为CONVERTED时发送此事件
 */
public class DocumentConvertedEvent extends ApplicationEvent {

    /**
     * 文档ID
     */
    private final Long documentId;

    public DocumentConvertedEvent(Object source, Long documentId, KnowledgeDocument document) {
        super(source);
        this.documentId = documentId;
    }

    public Long getDocumentId() {
        return documentId;
    }

    @Override
    public String toString() {
        return "DocumentConvertedEvent{documentId=" + documentId + '}';
    }
}
