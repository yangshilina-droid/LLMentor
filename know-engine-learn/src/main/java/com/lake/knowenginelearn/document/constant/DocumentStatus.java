package com.lake.knowenginelearn.document.constant;

/**
 * @author LAKE.YANG
 * @filename DocumentStatus
 * @date 2026-07-04 11:44
 */
public enum DocumentStatus {
    /**
     * 初始状态
     */
    INIT,
    /**
     * 上传完成
     */
    UPLOADED,
    /**
     * 转换中
     */
    CONVERTING,
    /**
     * 转换完成
     */
    CONVERTED,
    /**
     * 分块完成
     */
    CHUNKED,
    /**
     * 向量存储完成
     */
    VECTOR_STORED,
    /**
     * excel 数据逐行存储到数据库，不需要CONVERTED
     *
     * 后续状态流转 CHUNKED、VECTOR_STORED
     */
    STORED;
}
