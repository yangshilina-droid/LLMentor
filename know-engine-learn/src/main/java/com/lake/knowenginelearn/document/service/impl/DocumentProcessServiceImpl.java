package com.lake.knowenginelearn.document.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lake.knowenginelearn.document.constant.DocumentStatus;
import com.lake.knowenginelearn.document.constant.SegmentStatus;
import com.lake.knowenginelearn.document.entity.KnowledgeDocument;
import com.lake.knowenginelearn.document.entity.KnowledgeSegment;
import com.lake.knowenginelearn.document.event.DocumentChunkedEvent;
import com.lake.knowenginelearn.document.event.DocumentConvertedEvent;
import com.lake.knowenginelearn.document.service.*;
import com.lake.knowenginelearn.infra.lock.DistributeLock;
import com.lake.knowenginelearn.rag.modules.splitter.MarkdownHeaderParentTextSplitter;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 文档处理服务实现类 负责文档的业务流程处理：上传、转换、分段、向量化
 */
@Slf4j
@Service
public class DocumentProcessServiceImpl implements DocumentProcessService {

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileProcessService fileProcessService;

    @Autowired
    private KnowledgeSegmentService knowledgeSegmentService;

    @Autowired
    private ElasticsearchEmbeddingStore elasticsearchEmbeddingStore;

    @Autowired
    private OpenAiEmbeddingModel openAiEmbeddingModel;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Value("${minio.bucketName}")
    private String bucketName;

    private final Tika tika = new Tika();

    @Override
    @DistributeLock(scene = "document-upload", keyExpression = "#uploadUser", waitTime = 0)
    public KnowledgeDocument upload(MultipartFile file, String uploadUser, String accessibleBy) throws IOException {
        try {
            log.info("start to upload ....");
            String fileName = file.getOriginalFilename();
            // 用minio上传
            String fileUrl = fileStorageService.uploadFile(file, fileName);

            // 构建文档记录
            KnowledgeDocument document = new KnowledgeDocument();
            document.setDocTitle(fileName);
            document.setUploadUser(uploadUser);
            document.setDocUrl(fileUrl);
            document.setStatus(DocumentStatus.UPLOADED);
            document.setAccessibleBy(accessibleBy);

            // 保存到数据库
            boolean result = knowledgeDocumentService.save(document);
            Assert.isTrue(result, "文件上传失败");

            // 如果是 PDF 文件（通过后缀或文件头判断），调用转换处理
            if (isPdfFile(fileName) || isPdfContent(file)) {
                // 调用文件处理服务进行转换
                fileProcessService.processDocument(document, file.getInputStream());
            } else {
                // 非PDF文件，直接更新文档状态为已转换
                document.setStatus(DocumentStatus.CONVERTED);
                document.setConvertedDocUrl(fileUrl);
                result = knowledgeDocumentService.updateById(document);
                Assert.isTrue(result, "文件状态更新失败");
            }

            publishConvertedEvent(document);
            return document;
        } catch (Exception e) {
            throw new IOException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    @DistributeLock(scene = "document-split", keyExpression = "#document.docId", waitTime = 0)
    public int split(KnowledgeDocument document) {
        // 1. 查询文档
        Assert.notNull(document, "文档不存在");
        Assert.notNull(document.getConvertedDocUrl(), "文档未转换完成");

        if (document.getStatus() == DocumentStatus.CHUNKED) {
            // 返回已切分的分段数量
            Long chunkedCount = knowledgeSegmentService.count(new QueryWrapper<KnowledgeSegment>()
                    .eq("document_id", document.getDocId())
                    .eq("skipEmbedding", 0));
            return chunkedCount.intValue();
        }

        if (document.getStatus() != DocumentStatus.CONVERTED) {
            throw new RuntimeException("文档状态不为CONVERTED，无法完成切分");
        }

        // 2. 从MinIO下载文件内容
        String convertedDocUrl = document.getConvertedDocUrl();
        String objectName = extractObjectNameFromUrl(convertedDocUrl);
        Assert.notNull(objectName, "无法解析文档URL");

        String content;
        try (InputStream inputStream = fileStorageService.downloadFile(objectName)) {
            content = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("下载文档失败: " + e.getMessage(), e);
        }

        // 3. 使用 MarkdownHeaderParentTextSplitter 进行切分
        MarkdownHeaderParentTextSplitter splitter = new MarkdownHeaderParentTextSplitter(1000, 100);
        Document doc = Document.from(content);
        List<TextSegment> segments = splitter.split(doc);

        // 4. 转换为 KnowledgeSegment 并保存
        List<KnowledgeSegment> knowledgeSegments = new ArrayList<>();
        for (int i = 0; i < segments.size(); i++) {
            TextSegment segment = segments.get(i);
            KnowledgeSegment knowledgeSegment = new KnowledgeSegment();
            knowledgeSegment.setText(segment.text());
            knowledgeSegment.setChunkId(segment.metadata().getString("chunkId"));
            knowledgeSegment.setMetadata(JSON.toJSONString(segment.metadata().toMap()));
            knowledgeSegment.setDocumentId(document.getDocId());
            knowledgeSegment.setChunkOrder(i);

            // 检查是否需要跳过嵌入
            Integer skipEmbedding = segment.metadata().getInteger("skipEmbedding");
            if (skipEmbedding != null && skipEmbedding == 1) {
                knowledgeSegment.setSkipEmbedding(1);
                knowledgeSegment.setStatus(SegmentStatus.STORED);
            } else {
                knowledgeSegment.setSkipEmbedding(0);
                knowledgeSegment.setStatus(SegmentStatus.STORED);
            }

            knowledgeSegments.add(knowledgeSegment);
        }

        // 5. 批量保存片段
        boolean saveResult = knowledgeSegmentService.saveBatch(knowledgeSegments);
        Assert.isTrue(saveResult, "保存知识片段失败");

        int segmentCount = knowledgeSegments.size();

        // 6. 更新文档状态为 CHUNKED
        document.setStatus(DocumentStatus.CHUNKED);
        boolean updateResult = knowledgeDocumentService.updateById(document);
        Assert.isTrue(updateResult, "更新文档状态失败");

        // 发送文档已分段事件
        publishChunkedEvent(document, segmentCount);

        return segmentCount;
    }

    @Override
    @DistributeLock(scene = "document-embed", keyExpression = "#document.docId", waitTime = 0)
    public boolean embedAndStore(KnowledgeDocument document) {
        if (document == null) {
            return false;
        }

        if (document.getStatus() == DocumentStatus.VECTOR_STORED) {
            return true;
        }

        if (document.getStatus() != DocumentStatus.CHUNKED) {
            return false;
        }

        // 分页扫描全部document_id为docId且status为STORED的文档片段
        LambdaQueryWrapper<KnowledgeSegment> queryWrapper = Wrappers.<KnowledgeSegment>lambdaQuery()
                .eq(KnowledgeSegment::getDocumentId, document.getDocId())
                .eq(KnowledgeSegment::getStatus, SegmentStatus.STORED)
                .isNull(KnowledgeSegment::getEmbeddingId)
                .eq(KnowledgeSegment::getSkipEmbedding, 0);

        while (true) {
            /**
             * 已完成的分段会退出 queryWrapper 的查询结果，因此每轮固定读取第一页，避免翻页时跳过数据。
             *
             * 说明：
             * 每个片段处理成功后 会从status = STORED embedding_id = null 变为 status = VECTOR_STORED embedding_id != null
             * 会退出原查询结果集，导致剩余数据向前移动。此时继续查询第二页，就会跳过一部分已经移动到第一页的数据
             */
            Page<KnowledgeSegment> page = knowledgeSegmentService.page(new Page<>(1, 100), queryWrapper);
            List<KnowledgeSegment> textSegmentsToEmbed = page.getRecords();
            /**
             * 原来的 page.hasNext() 表示当前页之后是否还有下一页。当查询到最后一页时，它会返回 false，导致最后一页在进入循环处理前就被跳过
             * 新逻辑以“是否还有待处理记录”为终止条件，不会遗漏最后一批不足 100 条的数据
             */
            if (textSegmentsToEmbed.isEmpty()) {
                break;
            }

            List<TextSegment> textSegments = textSegmentsToEmbed.stream().map(segment -> TextSegment.from(segment.getText(), Metadata.from(segment.getMetadataMap()))).toList();
            // 获取嵌入向量
            Response<List<Embedding>> embeddingResponse = openAiEmbeddingModel.embedAll(textSegments);

            // 存储嵌入向量 es
            List<String> embeddingIds = elasticsearchEmbeddingStore.addAll(embeddingResponse.content(), textSegments);
            Assert.isTrue(embeddingIds.size() == textSegmentsToEmbed.size(),
                    "向量存储返回数量与文档分段数量不一致");

            //todo 事务处理

            // 更新文档片段状态
            for (int i = 0; i < textSegmentsToEmbed.size(); i++) {
                String embeddingId = embeddingIds.get(i);
                KnowledgeSegment knowledgeSegment = textSegmentsToEmbed.get(i);
                knowledgeSegment.setEmbeddingId(embeddingId);
                knowledgeSegment.setStatus(SegmentStatus.VECTOR_STORED);
                boolean updateResult = knowledgeSegmentService.updateById(knowledgeSegment);
                Assert.isTrue(updateResult, "更新知识片段向量状态失败，segmentId: " + knowledgeSegment.getId());
            }
        }

        //double check
        long segmentCount = knowledgeSegmentService.count(queryWrapper);
        if (segmentCount == 0) {
            // 更新文档状态
            document.setStatus(DocumentStatus.VECTOR_STORED);
            return knowledgeDocumentService.updateById(document);
        }

        log.warn("向量存储失败，存在部分分段没有存储成功，未成功的数量： " + segmentCount);
        return false;
    }

    // ==================== 事件发布方法 ====================

    /**
     * 发送文档已转换事件
     */
    private void publishConvertedEvent(KnowledgeDocument document) {
        log.info("发送文档CONVERTED事件，documentId: {}", document.getDocId());
        DocumentConvertedEvent event = new DocumentConvertedEvent(this, document.getDocId(), document);
        eventPublisher.publishEvent(event);
    }

    /**
     * 发送文档已分段事件
     */
    private void publishChunkedEvent(KnowledgeDocument document, int segmentCount) {
        log.info("发送文档CHUNKED事件，documentId: {}, segmentCount: {}", document.getDocId(), segmentCount);
        DocumentChunkedEvent event = new DocumentChunkedEvent(this, document.getDocId(), document, segmentCount);
        eventPublisher.publishEvent(event);
    }

    // ==================== 辅助方法 ====================

    /**
     * 通过后缀名判断是否为 PDF 文件
     */
    private boolean isPdfFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        return fileName.toLowerCase().endsWith(".pdf");
    }

    /**
     * 通过 Apache Tika 检测文件内容类型判断是否为 PDF 文件
     */
    private boolean isPdfContent(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            String mimeType = tika.detect(is);
            return "application/pdf".equals(mimeType);
        } catch (IOException e) {
            log.error("文件类型检测失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从MinIO URL中提取对象名称
     */
    private String extractObjectNameFromUrl(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        // URL格式: http://endpoint/bucketName/objectName
        int lastSlashIndex = url.lastIndexOf(bucketName) + bucketName.length();
        if (lastSlashIndex == -1 || lastSlashIndex == url.length() - 1) {
            return null;
        }
        return url.substring(lastSlashIndex + 1);
    }
}
