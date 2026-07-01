package com.lake.knowengine.document.service;

import com.lake.knowengine.document.entity.DocumentUploadParam;
import com.lake.knowengine.document.entity.KnowledgeDocument;

import java.io.IOException;

/**
 * 文档处理服务接口
 * 负责文档的业务流程处理：上传、转换、分段、向量化
 */
public interface DocumentProcessService {

    /**
     * 上传文件
     * @param documentUploadParam 上传参数
     * @param uploadUser 上传用户
     * @return 保存后的文档记录
     * @throws IOException IO异常
     */
    public KnowledgeDocument upload(DocumentUploadParam documentUploadParam, String uploadUser) throws IOException;



}
