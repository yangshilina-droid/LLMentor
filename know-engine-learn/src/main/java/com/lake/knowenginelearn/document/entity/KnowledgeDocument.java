package com.lake.knowenginelearn.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.lake.knowenginelearn.document.constant.DocumentStatus;
import lombok.Data;

import java.time.LocalDate;

/**
 * 知识文档表
 */
@Data
@TableName("knowledge_document")
public class KnowledgeDocument {

    /**
     * 文档ID
     */
    @TableId(value = "doc_id", type = IdType.AUTO)
    private Long docId;

    /**
     * 文档标题
     */
    private String docTitle;

    /**
     * 上传用户
     */
    private String uploadUser;

    /**
     * 文档URL
     */
    private String docUrl;

    /**
     * 转换后的文档URL
     */
    private String convertedDocUrl;

    /**
     * 文档失效日期
     */
    private LocalDate expireDate;

    /**
     * 状态：INIT, UPLOADED, CONVERTING, CONVERTED, CHUNKED, VECTOR_STORED
     */
    private DocumentStatus status;

    /**
     * 可见范围
     */
    private String accessibleBy;

    /**
     * 扩展字段，保存JSON字符串
     */
    private String extension;
}
