package com.lake.knowenginelearn.document.event;

import com.lake.knowenginelearn.document.entity.KnowledgeDocument;
import com.lake.knowenginelearn.document.service.DocumentProcessService;
import com.lake.knowenginelearn.document.service.KnowledgeDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 文档事件监听器
 * 负责处理文档状态变更后的后续流程
 */
@Slf4j
@Component
public class DocumentEventListener {

    @Autowired
    private DocumentProcessService documentProcessService;

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    /**
     * 监听文档CONVERTED事件
     * 触发文档分段流程
     *
     * @param event 文档已转换事件
     */
    @Async("eventListenerExecutor")
    @EventListener
    public void onDocumentConverted(DocumentConvertedEvent event) {
        Long documentId = event.getDocumentId();
        log.info("收到文档CONVERTED事件，开始执行文档分段，documentId: {}", documentId);

        try {
            KnowledgeDocument document = knowledgeDocumentService.getById(documentId);
            int segmentCount = documentProcessService.split(document);
            log.info("文档分段完成，documentId: {}, segmentCount: {}", documentId, segmentCount);
        } catch (Exception e) {
            log.error("文档分段失败，documentId: {}", documentId, e);
        }
    }

    /**
     * 监听文档CHUNKED事件
     * 触发向量嵌入流程
     *
     * @param event 文档已分段事件
     */
    @Async("eventListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onDocumentChunked(DocumentChunkedEvent event) {
        Long documentId = event.getDocumentId();
        log.info("收到文档CHUNKED事件，开始执行向量嵌入，documentId: {}, segmentCount: {}",
                documentId, event.getSegmentCount());

        try {
            KnowledgeDocument document = knowledgeDocumentService.getById(documentId);
            boolean success = documentProcessService.embedAndStore(document);
            log.info("向量嵌入完成，documentId: {}, success: {}", documentId, success);
        } catch (Exception e) {
            log.error("向量嵌入失败，documentId: {}", documentId, e);
        }
    }
}
