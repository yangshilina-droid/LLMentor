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
 * 文档处理服务实现类
 * 负责文档的业务流程处理：上传、转换、分段、向量化
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
    public KnowledgeDocument uploadFile(MultipartFile file, String uploadUser, String accessibleBy) throws IOException {
        try {
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

            // publishConvertedEvent(document);
            return document;
        } catch (Exception e) {
            throw new IOException("文件上传失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void processDocumentConversion(KnowledgeDocument document, InputStream inputStream) {
        Long documentId = document.getDocId();
        log.info("开始处理文档转换，documentId: {}", documentId);

        try {
            // 调用文件处理服务进行转换
            fileProcessService.processDocument(document, inputStream);

            // 转换完成后，重新查询文档状态
            KnowledgeDocument updatedDocument = knowledgeDocumentService.getById(documentId);
            if (updatedDocument != null && updatedDocument.getStatus() == DocumentStatus.CONVERTED) {
                // 发送文档已转换事件
                publishConvertedEvent(updatedDocument);
            }
        } catch (Exception e) {
            log.error("文档转换失败，documentId: {}", documentId, e);
            throw new RuntimeException("文档转换失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public int splitDocument(Long documentId) {
        //todo 加分布式锁
        // 1. 查询文档
        KnowledgeDocument document = knowledgeDocumentService.getById(documentId);
        Assert.notNull(document, "文档不存在");
        Assert.notNull(document.getConvertedDocUrl(), "文档未转换完成");

        if (document.getStatus() == DocumentStatus.CHUNKED) {
            // 返回已切分的分段数量
            Long chunkedCount = knowledgeSegmentService.count(new QueryWrapper<KnowledgeSegment>()
                    .eq("document_id", documentId)
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
            knowledgeSegment.setDocumentId(documentId);
            knowledgeSegment.setChunkOrder(i);
            knowledgeSegment.setStatus(SegmentStatus.INIT);

            // 检查是否需要跳过嵌入
            Integer skipEmbedding = segment.metadata().getInteger("skipEmbedding");
            if (skipEmbedding != null && skipEmbedding == 1) {
                knowledgeSegment.setSkipEmbedding(1);
            } else {
                knowledgeSegment.setSkipEmbedding(0);
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
    public boolean embeddingAndStore(Long docId) {
        // todo 增加分布式锁
        KnowledgeDocument knowledgeDocument = knowledgeDocumentService.getById(docId);
        if (knowledgeDocument == null) {
            return false;
        }

        if (knowledgeDocument.getStatus() == DocumentStatus.VECTOR_STORED) {
            return true;
        }

        // todo 状态的校验

        // 分页扫描全部document_id为docId且status为INIT的文档片段
        LambdaQueryWrapper<KnowledgeSegment> queryWrapper = Wrappers.<KnowledgeSegment>lambdaQuery()
                .eq(KnowledgeSegment::getDocumentId, docId)
                //todo 状态优化
                .eq(KnowledgeSegment::getStatus, SegmentStatus.INIT)
                .isNull(KnowledgeSegment::getEmbeddingId)
                .eq(KnowledgeSegment::getSkipEmbedding, 0);

        Page<KnowledgeSegment> page = knowledgeSegmentService.page(new Page<>(1, 100), queryWrapper);

        while (page.getCurrent() == 1 || page.hasNext()) {
            List<KnowledgeSegment> textSegmentsToEmbed = page.getRecords();
            List<TextSegment> textSegments = textSegmentsToEmbed.stream()
                    .map(segment -> TextSegment.from(segment.getText(), Metadata.from(segment.getMetadataMap())))
                    .toList();
            // 获取嵌入向量
            Response<List<Embedding>> embeddingResponse = openAiEmbeddingModel.embedAll(textSegments);

            // 存储嵌入向量
            List<String> embeddingIds = elasticsearchEmbeddingStore.addAll(embeddingResponse.content(), textSegments);

            // 更新文档片段状态
            for (int i = 0; i < textSegmentsToEmbed.size(); i++) {
                String embeddingId = embeddingIds.get(i);
                KnowledgeSegment knowledgeSegment = textSegmentsToEmbed.get(i);
                knowledgeSegment.setEmbeddingId(embeddingId);
                knowledgeSegment.setStatus(SegmentStatus.VECTOR_STORED);
                knowledgeSegmentService.updateById(knowledgeSegment);
            }

            // 继续扫描下一页
            page = knowledgeSegmentService.page(new Page<>(page.getCurrent() + 1, 100), queryWrapper);
        }

        // todo 需要对所有的segment做检查，确保所有的segment都已转换为vector

        // 更新文档状态
        knowledgeDocument.setStatus(DocumentStatus.VECTOR_STORED);
        boolean result = knowledgeDocumentService.updateById(knowledgeDocument);

        return result;
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



