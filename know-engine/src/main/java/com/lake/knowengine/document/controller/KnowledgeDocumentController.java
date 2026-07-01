package com.lake.knowengine.document.controller;

import com.lake.knowengine.document.entity.DocumentUploadParam;
import com.lake.knowengine.document.entity.KnowledgeDocument;
import com.lake.knowengine.document.service.DocumentProcessService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/document")
public class KnowledgeDocumentController {

    @Autowired
    private DocumentProcessService documentProcessService;

    /**
     * 文件上传接口
     *
     * @param file         上传的文件
     * @param accessibleBy 可见范围（可选）
     * @return 保存后的文档记录
     */
    @PostMapping("/upload")
    public KnowledgeDocument uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam(value = "version", required = false, defaultValue = "1.0.0") String version,
            @RequestParam(value = "tableName", required = false) String tableName,
            @RequestParam("description") String description,
            @RequestParam("knowledgeBaseType") String knowledgeBaseType,
            @RequestParam(value = "accessibleBy", required = false) String accessibleBy) throws IOException {
        // String uploadUser = authService.getCurrentUser().getName();
        return documentProcessService.upload(new DocumentUploadParam(file, title, accessibleBy, description, knowledgeBaseType, tableName, version), "uploadUser");
    }
}
