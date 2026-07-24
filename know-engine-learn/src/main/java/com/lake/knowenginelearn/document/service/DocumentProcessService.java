package com.lake.knowenginelearn.document.service;


import com.lake.knowenginelearn.document.entity.DocumentSplitParam;
import com.lake.knowenginelearn.document.entity.DocumentUploadParam;
import com.lake.knowenginelearn.document.entity.KnowledgeDocument;

import java.io.IOException;

/**
 * 文档处理服务接口
 * 负责文档的业务流程处理：上传、转换、分段、向量化
 */
public interface DocumentProcessService {

    /**
     * 上传文件
     * @return 保存后的文档记录
     * @throws IOException IO异常
     */
    public KnowledgeDocument upload(DocumentUploadParam documentUploadParam) throws IOException;

    /**
     * 对文档进行切分
     * 使用 MarkdownHeaderParentTextSplitter 进行切分
     *
     * @param document 文档ID
     * @return 切分后的片段数量
     */
    public int split(KnowledgeDocument document, DocumentSplitParam documentSplitParam);

    /**
     * 向量化并存储
     *
     * @return 是否成功
     */
    boolean embedAndStore(KnowledgeDocument knowledgeDocument);
}

