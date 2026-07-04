package com.lake.knowenginelearn.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lake.knowenginelearn.document.constant.DocumentStatus;
import com.lake.knowenginelearn.document.entity.KnowledgeDocument;
import com.lake.knowenginelearn.document.service.FileProcessService;
import com.lake.knowenginelearn.document.service.FileStorageService;
import com.lake.knowenginelearn.document.service.KnowledgeDocumentService;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/document")
public class KnowledgeDocumentController {

    @Autowired
    private KnowledgeDocumentService knowledgeDocumentService;

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
     */
    @PostMapping("/upload")
    public KnowledgeDocument uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("uploadUser") String uploadUser,
            @RequestParam(value = "accessibleBy", required = false) String accessibleBy) throws IOException {

        //用minio上传
        try {
            String fileName = file.getOriginalFilename();
            String fileUrl = fileStorageService.uploadFile(file, fileName);

            // 4. 构建文档记录
            KnowledgeDocument document = new KnowledgeDocument();
            document.setDocTitle(fileName);
            document.setUploadUser(uploadUser);
            document.setDocUrl(fileUrl);
            document.setStatus(DocumentStatus.UPLOADED);
            //todo permission处理
            document.setAccessibleBy(accessibleBy);

            // 5. 保存到数据库
            knowledgeDocumentService.save(document);

            // 6. 如果是 PDF 文件（通过后缀或文件头判断），异步调用转换处理
            if (isPdfFile(fileName) || isPdfContent(file)) {
                try {
                    fileProcessService.processDocument(document);
                } catch (Exception e) {
                    // 转换失败不影响上传结果，仅记录日志
                    System.err.println("PDF 转换处理失败，documentId: " + document.getDocId() + ", error: " + e.getMessage());
                }
            }

            return document;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 分页查询
     */
    @GetMapping("/page")
    public Page<KnowledgeDocument> page(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        return knowledgeDocumentService.page(new Page<>(current, size));
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    public KnowledgeDocument getById(@PathVariable Long id) {
        return knowledgeDocumentService.getById(id);
    }

    /**
     * 根据状态查询列表
     */
    @GetMapping("/list-by-status")
    public List<KnowledgeDocument> listByStatus(@RequestParam String status) {
        QueryWrapper<KnowledgeDocument> wrapper = new QueryWrapper<>();
        wrapper.eq("status", status);
        return knowledgeDocumentService.list(wrapper);
    }

    /**
     * 根据ID删除
     */
    @DeleteMapping("/{id}")
    public boolean removeById(@PathVariable Long id) {
        return knowledgeDocumentService.removeById(id);
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
