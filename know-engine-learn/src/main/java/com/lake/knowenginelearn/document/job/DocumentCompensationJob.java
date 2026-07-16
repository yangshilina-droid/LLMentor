package com.lake.knowenginelearn.document.job;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lake.knowenginelearn.document.constant.DocumentStatus;
import com.lake.knowenginelearn.document.constant.SegmentStatus;
import com.lake.knowenginelearn.document.entity.KnowledgeDocument;
import com.lake.knowenginelearn.document.entity.KnowledgeSegment;
import com.lake.knowenginelearn.document.service.DocumentProcessService;
import com.lake.knowenginelearn.document.service.KnowledgeDocumentService;
import com.lake.knowenginelearn.document.service.KnowledgeSegmentService;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 文档处理补偿任务
 * 用于处理事件处理失败后的补偿逻辑
 */
@Slf4j
@Component
public class DocumentCompensationJob {

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private KnowledgeSegmentService knowledgeSegmentService;

    @Autowired
    private DocumentProcessService documentProcessService;

    /**
     * 最大重试次数
     */
    private static final int MAX_RETRY_COUNT = 5;

    /**
     * 补偿阈值（分钟）- 超过此时间才进行补偿
     */
    private static final int COMPENSATION_THRESHOLD_MINUTES = 5;

    /**
     * 文档分段补偿任务
     * 扫描 CONVERTED 状态超过阈值的文档，重新触发分段
     */
    @XxlJob("documentSplitCompensation")
    public void documentSplitCompensation() {
        log.info("========== 开始执行文档分段补偿任务 ==========");
        int successCount = 0;
        int failCount = 0;

        try {
            // 查询 CONVERTED 状态的文档
            // todo 注：实际项目中应该在实体和数据库中添加 updateTime 和 retryCount 字段
            // 这里简化处理，查询所有 CONVERTED 状态的文档
            LambdaQueryWrapper<KnowledgeDocument> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(KnowledgeDocument::getStatus, DocumentStatus.CONVERTED);
            queryWrapper.isNotNull(KnowledgeDocument::getConvertedDocUrl);

            List<KnowledgeDocument> documents = knowledgeDocumentService.list(queryWrapper);
            log.info("发现 {} 个待补偿的 CONVERTED 状态文档", documents.size());

            for (KnowledgeDocument document : documents) {
                try {
                    // 检查重试次数（从 extension 字段解析，或使用默认值）
                    int retryCount = getRetryCount(document);
                    if (retryCount >= MAX_RETRY_COUNT) {
                        log.warn("文档 {} 已达最大重试次数 {}，跳过补偿", document.getDocId(), retryCount);
                        continue;
                    }

                    log.info("补偿处理文档分段，documentId: {}, retryCount: {}", document.getDocId(), retryCount);

                    // 执行分段
                    //todo 多余查询?
                    int segmentCount = documentProcessService.splitDocument(document.getDocId());

                    // 更新重试次数
                    updateRetryCount(document.getDocId(), retryCount + 1);

                    log.info("文档分段补偿成功，documentId: {}, segmentCount: {}", document.getDocId(), segmentCount);
                    successCount++;
                } catch (Exception e) {
                    log.error("文档分段补偿失败，documentId: {}", document.getDocId(), e);
                    failCount++;
                }
            }
        } catch (Exception e) {
            log.error("文档分段补偿任务执行异常", e);
        }

        log.info("========== 文档分段补偿任务完成，成功: {}，失败: {} ==========", successCount, failCount);
    }

    /**
     * 向量化补偿任务
     * 扫描 CHUNKED 状态但存在未向量化的 segment，重新触发向量化
     */
    @XxlJob("documentEmbeddingCompensation")
    public void documentEmbeddingCompensation() {
        log.info("========== 开始执行向量化补偿任务 ==========");
        int successCount = 0;
        int failCount = 0;

        try {
            // 查询 CHUNKED 状态的文档
            //todo 扫表注意索引问题
            LambdaQueryWrapper<KnowledgeDocument> docQueryWrapper = new LambdaQueryWrapper<>();
            docQueryWrapper.eq(KnowledgeDocument::getStatus, DocumentStatus.CHUNKED);

            List<KnowledgeDocument> documents = knowledgeDocumentService.list(docQueryWrapper);
            log.info("发现 {} 个 CHUNKED 状态的文档", documents.size());

            for (KnowledgeDocument document : documents) {
                try {
                    // 检查是否存在未向量化的片段
                    LambdaQueryWrapper<KnowledgeSegment> segmentQueryWrapper = new LambdaQueryWrapper<>();
                    segmentQueryWrapper.eq(KnowledgeSegment::getDocumentId, document.getDocId());
                    segmentQueryWrapper.eq(KnowledgeSegment::getStatus, SegmentStatus.INIT);
                    segmentQueryWrapper.eq(KnowledgeSegment::getSkipEmbedding, 0);
                    segmentQueryWrapper.isNull(KnowledgeSegment::getEmbeddingId);

                    //todo 这段逻辑可以内置到embeddingAndStore
                    long unembeddedCount = knowledgeSegmentService.count(segmentQueryWrapper);
                    if (unembeddedCount == 0) {
                        // 没有未向量化的片段，检查是否所有片段都已处理
                        // 如果所有片段都已向量化，更新文档状态
                        checkAndUpdateDocumentStatus(document.getDocId());
                        continue;
                    }

                    // 检查重试次数
                    int retryCount = getRetryCount(document);
                    if (retryCount >= MAX_RETRY_COUNT) {
                        log.warn("文档 {} 已达最大重试次数 {}，跳过补偿", document.getDocId(), retryCount);
                        continue;
                    }

                    log.info("补偿处理向量化，documentId: {}, unembeddedCount: {}, retryCount: {}",
                            document.getDocId(), unembeddedCount, retryCount);

                    // 执行向量化
                    boolean success = documentProcessService.embeddingAndStore(document.getDocId());

                    if (success) {
                        // 更新重试次数
                        updateRetryCount(document.getDocId(), retryCount + 1);
                        log.info("向量化补偿成功，documentId: {}", document.getDocId());
                        successCount++;
                    } else {
                        log.warn("向量化补偿失败，documentId: {}", document.getDocId());
                        failCount++;
                    }
                } catch (Exception e) {
                    log.error("向量化补偿失败，documentId: {}", document.getDocId(), e);
                    failCount++;
                }
            }
        } catch (Exception e) {
            log.error("向量化补偿任务执行异常", e);
        }

        log.info("========== 向量化补偿任务完成，成功: {}，失败: {} ==========", successCount, failCount);
    }

    /**
     * 检查并更新文档状态
     * 如果所有片段都已向量化，更新文档状态为 VECTOR_STORED
     */
    private void checkAndUpdateDocumentStatus(Long documentId) {
        // 检查是否还有未向量化的片段
        LambdaQueryWrapper<KnowledgeSegment> unembeddedQuery = new LambdaQueryWrapper<>();
        unembeddedQuery.eq(KnowledgeSegment::getDocumentId, documentId);
        unembeddedQuery.eq(KnowledgeSegment::getSkipEmbedding, 0);
        unembeddedQuery.and(wrapper -> wrapper
                .eq(KnowledgeSegment::getStatus, SegmentStatus.INIT)
                .or()
                .isNull(KnowledgeSegment::getEmbeddingId));

        long unembeddedCount = knowledgeSegmentService.count(unembeddedQuery);
        if (unembeddedCount == 0) {
            // 所有片段都已向量化，更新文档状态
            KnowledgeDocument document = knowledgeDocumentService.getById(documentId);
            if (document != null && document.getStatus() != DocumentStatus.VECTOR_STORED) {
                document.setStatus(DocumentStatus.VECTOR_STORED);
                knowledgeDocumentService.updateById(document);
                log.info("文档状态更新为 VECTOR_STORED，documentId: {}", documentId);
            }
        }
    }

    /**
     * 从 extension 字段获取重试次数
     */
    private int getRetryCount(KnowledgeDocument document) {
        String extension = document.getExtension();
        if (extension == null || extension.isEmpty()) {
            return 0;
        }
        try {
            com.alibaba.fastjson2.JSONObject json = com.alibaba.fastjson2.JSON.parseObject(extension);
            return json.getIntValue("retryCount");
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 更新重试次数到 extension 字段
     */
    private void updateRetryCount(Long documentId, int retryCount) {
        KnowledgeDocument document = knowledgeDocumentService.getById(documentId);
        if (document == null) {
            return;
        }

        com.alibaba.fastjson2.JSONObject json;
        String extension = document.getExtension();
        if (extension == null || extension.isEmpty()) {
            json = new com.alibaba.fastjson2.JSONObject();
        } else {
            try {
                json = com.alibaba.fastjson2.JSON.parseObject(extension);
            } catch (Exception e) {
                json = new com.alibaba.fastjson2.JSONObject();
            }
        }

        json.put("retryCount", retryCount);
        json.put("lastRetryTime", LocalDateTime.now().toString());

        document.setExtension(json.toJSONString());
        knowledgeDocumentService.updateById(document);
    }
}