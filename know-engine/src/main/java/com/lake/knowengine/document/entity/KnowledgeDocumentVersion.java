package com.lake.knowengine.document.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("knowledge_document_version")
public class KnowledgeDocumentVersion extends BaseEntity {

    /**
     * 版本ID
     */
    @TableId(value = "version_id", type = IdType.AUTO)
    private Long versionId;

    /**
     * 关联文档ID（knowledge_document.doc_id）
     */
    private Long docId;

    /**
     * 版本号（语义化版本，如 1.0.0）
     */
    private String version;

    /**
     * 该版本文档URL（MinIO原始文件）
     */
    private String docUrl;

    /**
     * 该版本转换后的文档URL
     */
    private String convertedDocUrl;

    /**
     * 该版本文档内容哈希值（SHA-256）
     */
    private String contentHash;

    /**
     * 版本状态：UPLOADED, CONVERTING, CONVERTED, CHUNKED, VECTOR_STORED, STORED
     */
    private String status;

    /**
     * 该版本上传用户
     */
    private String uploadUser;

    /**
     * 版本变更说明
     */
    private String changelog;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 修改时间
     */
    private LocalDateTime updatedAt;

    /**
     * 乐观锁版本号
     */
    @Version
    private Integer lockVersion;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
}
