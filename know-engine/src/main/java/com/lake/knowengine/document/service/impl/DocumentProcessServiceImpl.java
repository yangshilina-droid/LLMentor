package com.lake.knowengine.document.service.impl;

import com.lake.knowengine.document.constant.DocumentStatus;
import com.lake.knowengine.document.constant.FileType;
import com.lake.knowengine.document.entity.DocumentUploadParam;
import com.lake.knowengine.document.entity.KnowledgeDocument;
import com.lake.knowengine.document.entity.KnowledgeDocumentVersion;
import com.lake.knowengine.document.mapper.KnowledgeSegmentMapper;
import com.lake.knowengine.document.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
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
    private KnowledgeDocumentVersionService knowledgeDocumentVersionService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private KnowledgeSegmentService knowledgeSegmentService;

    @Autowired
    private KnowledgeSegmentMapper knowledgeSegmentMapper;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private List<FileProcessService> fileProcessServices;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Override
    // @DistributeLock(scene = "document-upload", keyExpression = "#uploadUser", waitTime = 0)
    public KnowledgeDocument upload(DocumentUploadParam documentUploadParam, String uploadUser) throws IOException {
        // 计算文件内容hash，用于去重
        String contentHash = calculateContentHash(documentUploadParam.file());

        // 检查是否已存在相同内容的版本（跨文档跨版本去重）
        if (knowledgeDocumentVersionService.existsByContentHash(contentHash)) {
            throw new IllegalArgumentException("文档内容已存在，请勿重复上传");
        }

        // 创建文档记录
        KnowledgeDocument document = new KnowledgeDocument().create(documentUploadParam);
        boolean result = knowledgeDocumentService.save(document);
        Assert.isTrue(result, "文件上传失败");


        log.info("start to upload ....");
        String fileName = documentUploadParam.file().getOriginalFilename();
        // 用minio上传
        String fileUrl = null;
        try {
            fileUrl = fileStorageService.uploadFile(documentUploadParam.file(), fileName);
        } catch (Exception e) {
            knowledgeDocumentService.removeDocumentWithSegments(document.getDocId());
            log.info("文件上传失败，文档已删除");
            return null;
        }

        // 创建初始版本记录
        KnowledgeDocumentVersion versionRecord = knowledgeDocumentVersionService.createVersionRecord(
                document.getDocId(), documentUploadParam.version(), fileUrl, null,
                uploadUser, contentHash, DocumentStatus.UPLOADED, null);
        document.setCurrentVersionId(versionRecord.getVersionId());

        // 处理文档（转换/存储），获取转换后的文档URL
        String convertedDocUrl = processFile(fileName, documentUploadParam.file(), document);

        // 更新版本记录的转换后URL
        versionRecord = knowledgeDocumentVersionService.getById(versionRecord.getVersionId());
        versionRecord.setConvertedDocUrl(convertedDocUrl);
        result = knowledgeDocumentVersionService.updateById(versionRecord);
        Assert.isTrue(result, "版本记录更新失败");

        KnowledgeDocument documentInDb = knowledgeDocumentService.getById(document.getDocId());
        documentInDb.setCurrentVersionId(versionRecord.getVersionId());
        result = knowledgeDocumentService.updateById(documentInDb);
        Assert.isTrue(result, "文档当前版本更新失败");

        return document;
    }

    /**
     * 计算文件内容的SHA-256哈希值
     *
     * @param file 上传的文件
     * @return SHA-256哈希的十六进制字符串
     */
    private String calculateContentHash(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256算法不可用", e);
        }
    }

    private String processFile(String fileName, MultipartFile file, KnowledgeDocument document) throws IOException {
        FileType fileType = getFileType(fileName);
        FileProcessService fileProcessService = fileProcessServices.stream()
                .filter(processService -> processService.supports(fileType, document.getKnowledgeBaseType()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的文件类型或知识库类型"));
        return fileProcessService.processDocument(document, file.getInputStream());
    }

    private FileType getFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return FileType.UNKNOWN;
        }
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
        try {
            return FileType.valueOf(extension);
        } catch (IllegalArgumentException e) {
            return FileType.UNKNOWN;
        }
    }

}
