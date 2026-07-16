package com.lake.knowenginelearn.document.service;


import com.lake.knowenginelearn.document.entity.KnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档处理服务接口
 * 负责文档的业务流程处理：上传、转换、分段、向量化
 */
public interface DocumentProcessService {

    /**
     * 上传文件
     *
     * @param file         上传的文件
     * @param uploadUser   上传用户
     * @param accessibleBy 可见范围（可选）
     * @return 保存后的文档记录
     * @throws IOException IO异常
     */
    KnowledgeDocument uploadFile(MultipartFile file, String uploadUser, String accessibleBy) throws IOException;

    /**
     * 对文档进行切分
     * 使用 MarkdownHeaderParentTextSplitter 进行切分
     *
     * @param documentId 文档ID
     * @return 切分后的片段数量
     */
    int splitDocument(Long documentId);

    /**
     * 向量化并存储
     *
     * @param docId 文档ID
     * @return 是否成功
     */
    boolean embeddingAndStore(Long docId);

    /**
     * 处理文档转换（PDF等转换为Markdown）
     *
     * @param document   文档对象
     * @param inputStream 文件输入流
     */
    void processDocumentConversion(KnowledgeDocument document, InputStream inputStream);
}

