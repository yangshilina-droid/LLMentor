package com.lake.knowenginelearn.document.controller;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.lake.knowenginelearn.document.constant.DocumentStatus;
import com.lake.knowenginelearn.document.constant.SegmentStatus;
import com.lake.knowenginelearn.document.entity.KnowledgeDocument;
import com.lake.knowenginelearn.document.entity.KnowledgeSegment;
import com.lake.knowenginelearn.document.service.FileProcessService;
import com.lake.knowenginelearn.document.service.FileStorageService;
import com.lake.knowenginelearn.document.service.KnowledgeDocumentService;
import com.lake.knowenginelearn.document.service.KnowledgeSegmentService;
import com.lake.knowenginelearn.rag.modules.splitter.MarkdownHeaderParentTextSplitter;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.segment.TextSegment;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/document")
public class KnowledgeDocumentController {

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

    @Autowired
    private KnowledgeSegmentService knowledgeSegmentService;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileProcessService fileProcessService;

    private final Tika tika = new Tika();

    /**
     * 文件上传接口
     *
     * @param file         上传的文件
     * @param uploadUser   上传用户
     * @param accessibleBy 可见范围（可选）
     * @return 保存后的文档记录
     *
     * 文件转换
     * -pdf文件需要使用MinerU转换为md文件
     * -其他文件，如word 不用转换
     *
     */
    @PostMapping("/upload")
    public KnowledgeDocument uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadUser") String uploadUser,
            @RequestParam(value = "accessibleBy", required = false) String accessibleBy) throws IOException {

        try {
            // 用minio上传
            String fileName = file.getOriginalFilename();
            String fileUrl = fileStorageService.uploadFile(file, fileName);

            // 构建文档记录
            KnowledgeDocument document = new KnowledgeDocument();
            document.setDocTitle(fileName);
            document.setUploadUser(uploadUser);
            document.setDocUrl(fileUrl);
            document.setStatus(DocumentStatus.UPLOADED);
            //todo permission处理
            document.setAccessibleBy(accessibleBy);

            // 保存到数据库
            knowledgeDocumentService.save(document);

            // 如果是 PDF 文件（通过后缀或文件头判断），异步调用转换处理
            if (isPdfFile(fileName) || isPdfContent(file)) {
                try {
                    fileProcessService.processDocument(document, file.getInputStream());
                } catch (Exception e) {
                    // 转换失败不影响上传结果，仅记录日志
                    System.err.println("PDF 转换处理失败，documentId: " + document.getDocId() + ", error: " + e.getMessage());
                }
            }
            // 非pdf文件，更新文档状态为已转换
            else {
                document.setStatus(DocumentStatus.CONVERTED);
                document.setConvertedDocUrl(fileUrl);
                knowledgeDocumentService.updateById(document);
            }

            return document;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 对文档进行切分，使用 MarkdownHeaderParentTextSplitter
     *
     * @param documentId 文档ID
     * @return 切分后的片段数量
     */
    @PostMapping("/split/{documentId}")
    @Transactional
    public Integer splitDocument(@PathVariable Long documentId) {
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

        if(document.getStatus() != DocumentStatus.CONVERTED){
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

        // 3. 使用 MarkdownHeaderParentTextSplitter 进行切分 langchain4j
        // 根据经验设置chunkSize, overlap
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
            //todo metadata 还需要增加更多的东西
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
        //todo 性能问题优化
        boolean saveResult = knowledgeSegmentService.saveBatch(knowledgeSegments);
        Assert.isTrue(saveResult, "保存知识片段失败");

        // 6. 更新文档状态为 CHUNKED
        document.setStatus(DocumentStatus.CHUNKED);
        boolean updateResult = knowledgeDocumentService.updateById(document);
        Assert.isTrue(updateResult, "更新文档状态失败");

        return knowledgeSegments.size();
    }

    @PostMapping("/embedding")
    public String embedding(Long docId) {
        return knowledgeDocumentService.embeddingAndStore(docId) ? "success" : "failed";
    }

    /**
     * 从MinIO URL中提取对象名称
     *
     * @param url MinIO文件URL
     * @return 对象名称
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


    /**
     * 通过后缀名判断是否为 PDF 文件
     * @param fileName 文件名
     * @return true 如果是 PDF 文件
     */
    private boolean isPdfFile(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }
        return fileName.toLowerCase().endsWith(".pdf");
    }

    /**
     * 通过 Apache Tika 检测文件内容类型判断是否为 PDF 文件
     * @param file 上传的文件
     * @return true 如果是 PDF 文件
     */
    private boolean isPdfContent(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            String mimeType = tika.detect(is);
            return "application/pdf".equals(mimeType);
        } catch (IOException e) {
            System.err.println("文件类型检测失败: " + e.getMessage());
            return false;
        }
    }
}
