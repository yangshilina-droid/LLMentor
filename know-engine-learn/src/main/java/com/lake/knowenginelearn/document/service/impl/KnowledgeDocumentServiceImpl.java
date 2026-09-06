package com.lake.knowenginelearn.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowenginelearn.document.constant.DocumentStatus;
import com.lake.knowenginelearn.document.constant.SegmentStatus;
import com.lake.knowenginelearn.document.entity.KnowledgeDocument;
import com.lake.knowenginelearn.document.entity.KnowledgeDocumentVersion;
import com.lake.knowenginelearn.document.entity.KnowledgeSegment;
import com.lake.knowenginelearn.document.mapper.KnowledgeDocumentMapper;
import com.lake.knowenginelearn.document.mapper.KnowledgeDocumentVersionMapper;
import com.lake.knowenginelearn.document.mapper.KnowledgeSegmentMapper;
import com.lake.knowenginelearn.document.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.Iterator;
import java.util.List;

/**
 * 知识文档表 Service 实现类
 */
@Slf4j
@Service
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument> implements
        KnowledgeDocumentService {

    @Autowired
    private KnowledgeSegmentMapper knowledgeSegmentMapper;

    @Autowired
    private KnowledgeDocumentVersionMapper knowledgeDocumentVersionMapper;

    @Autowired
    private KnowledgeSegmentService knowledgeSegmentService;

    @Autowired
    private KnowledgeDocumentMapper knowledgeDocumentMapper;

    @Autowired
    private KnowledgeDocumentVersionService knowledgeDocumentVersionService;

    @Autowired
    private VectorStoreService vectorStoreService;

    /**
     * 删除文档，并级联物理删除该文档下的所有分段和版本，同时按 docId 清除向量存储中的数据
     *
     * @param docId 文档ID
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDocumentWithSegments(Long docId) {
        // 按 metadata 中的 docId 删除该文档所有向量
        deleteVectorsByDocId(docId);

        // 物理删除该文档下的所有分段
        knowledgeSegmentMapper.physicalDeleteByDocumentId(docId);

        // 物理删除该文档的所有版本记录
        knowledgeDocumentVersionMapper.physicalDeleteByDocId(docId);

        // 物理删除文档本身
        return baseMapper.physicalDeleteByDocId(docId) > 0;
    }

    /**
     * 批量删除文档，并级联物理删除这些文档下的所有分段和版本，同时按 docId 批量清除向量存储中的数据
     *
     * @param docIds 文档ID列表
     * @return 是否删除成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDocumentsWithSegments(List<Long> docIds) {
        if (docIds == null || docIds.isEmpty()) {
            return false;
        }
        // 按 metadata 中的 docId 批量删除所有向量
        deleteVectorsByDocIds(docIds);

        // 物理删除这些文档下的所有分段
        knowledgeSegmentMapper.physicalDeleteByDocumentIds(docIds);

        // 物理删除这些文档的所有版本记录
        knowledgeDocumentVersionMapper.physicalDeleteByDocIds(docIds);

        // 物理删除文档本身
        return baseMapper.physicalDeleteByDocIds(docIds) > 0;
    }

    /**
     * 按 metadata 中的 docId 删除该文档所有向量
     */
    private void deleteVectorsByDocId(Long docId) {
        vectorStoreService.removeByDocId(docId);
    }

    /**
     * 按 metadata 中的 docId 批量删除向量
     */
    private void deleteVectorsByDocIds(List<Long> docIds) {
        vectorStoreService.removeByDocIds(docIds);
    }

    @Autowired
    private DocumentCleanupService documentCleanupService;

    /**
     * 让指定版本失效：
     * 1. 按 docId + versionId 清理 ES 向量
     * 2. 将该版本下所有分段状态从 VECTOR_STORED 降为 STORED，并清空 embeddingId
     * 3. 将版本记录状态从 VECTOR_STORED 降为 CHUNKED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateVersion(Long versionId) {
        KnowledgeDocumentVersion version = knowledgeDocumentVersionService.getById(versionId);
        Assert.notNull(version, "版本记录不存在: versionId=" + versionId);
        if (version.getStatus() == DocumentStatus.CHUNKED) {
            return;
        }

        Assert.isTrue(DocumentStatus.VECTOR_STORED == version.getStatus(),
                "版本状态不是 VECTOR_STORED，无法执行失效操作，当前状态: " + version.getStatus());

        Long docId = version.getDocId();
        log.info("开始让版本失效, docId={}, versionId={}", docId, versionId);

        // 1. 按 docId + versionId 清理 ES 向量
        documentCleanupService.cleanupOldVersionData(docId, versionId);

        // 2. 将该版本下所有分段状态从 VECTOR_STORED 降为 STORED，并清空 embeddingId
        LambdaUpdateWrapper<KnowledgeSegment> segUpdate = Wrappers.<KnowledgeSegment>lambdaUpdate()
                .set(KnowledgeSegment::getStatus, SegmentStatus.STORED)
                .set(KnowledgeSegment::getEmbeddingId, null)
                .eq(KnowledgeSegment::getDocumentId, docId)
                .eq(KnowledgeSegment::getDocumentVersion, versionId)
                .eq(KnowledgeSegment::getStatus, SegmentStatus.VECTOR_STORED);
        int affected = knowledgeSegmentMapper.update(null, segUpdate);
        log.info("降级分段状态完成, versionId={}, affected={}", versionId, affected);

        // 3. 将版本记录状态从 VECTOR_STORED 降为 CHUNKED
        version.setStatus(DocumentStatus.CHUNKED);
        boolean versionUpdateResult = knowledgeDocumentVersionService.updateById(version);
        Assert.isTrue(versionUpdateResult, "文档版本状态更新失败");
        log.info("版本失效完成, versionId={}", versionId);
    }

    /**
     * 让指定版本生效（重新向量化）：
     * 1. 校验版本状态必须为 CHUNKED
     * 2. 对该版本下所有 STORED 且未向量化的分段分批 embed 并写入 ES
     * 3. 更新分段状态为 VECTOR_STORED
     * 4. 将版本记录状态从 CHUNKED 升为 VECTOR_STORED
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activateVersion(Long versionId) {
        KnowledgeDocumentVersion version = knowledgeDocumentVersionService.getById(versionId);
        Assert.notNull(version, "版本记录不存在: versionId=" + versionId);

        if (version.getStatus() == DocumentStatus.VECTOR_STORED) {
            return;
        }

        Assert.isTrue(DocumentStatus.CHUNKED == version.getStatus(),
                "版本状态不是 CHUNKED，无法执行生效操作，当前状态: " + version.getStatus());

        Long docId = version.getDocId();
        log.info("开始让版本生效（重新向量化）, docId={}, versionId={}", docId, versionId);

        // 分页扫描 STORED 且未向量化的分段（skipEmbedding=0）
        LambdaQueryWrapper<KnowledgeSegment> queryWrapper = Wrappers.<KnowledgeSegment>lambdaQuery()
                .eq(KnowledgeSegment::getDocumentId, docId)
                .eq(KnowledgeSegment::getDocumentVersion, versionId)
                .eq(KnowledgeSegment::getStatus, SegmentStatus.STORED)
                .eq(KnowledgeSegment::getSkipEmbedding, 0)
                .isNull(KnowledgeSegment::getEmbeddingId);

        Page<KnowledgeSegment> page = knowledgeSegmentService.page(new Page<>(1, 100), queryWrapper);
        while (!page.getRecords().isEmpty()) {
            List<KnowledgeSegment> batch = page.getRecords();
            List<String> embeddingIds = vectorStoreService.embedAndStore(batch);

            for (int i = 0; i < batch.size(); i++) {
                KnowledgeSegment seg = batch.get(i);
                seg.setEmbeddingId(embeddingIds.get(i));
                seg.setStatus(SegmentStatus.VECTOR_STORED);
                boolean updateResult = knowledgeSegmentService.updateById(seg);
                Assert.isTrue(updateResult, "分段更新失败: " + seg.getId());
            }

            page = knowledgeSegmentService.page(new Page<>(page.getCurrent(), 100), queryWrapper);
        }

        // 将版本记录状态升为 VECTOR_STORED
        version.setStatus(DocumentStatus.VECTOR_STORED);
        boolean result = knowledgeDocumentVersionService.updateById(version);
        Assert.isTrue(result, "版本记录更新失败: " + versionId);
        log.info("版本生效完成, versionId={}", versionId);
    }

    @Override
    public List<KnowledgeDocument> scanDocumentsNeedingCleanup() {
        // 查询所有状态为 VECTOR_STORED 且有 currentVersionId 的文档
        QueryWrapper<KnowledgeDocument> docWrapper = new QueryWrapper<>();
        docWrapper.eq("status", DocumentStatus.VECTOR_STORED.name())
                .isNotNull("current_version_id");
        List<KnowledgeDocument> documents = this.list(docWrapper);

        // 使用 Iterator 安全删除无残留旧版本分段的文档，避免 ConcurrentModificationException 与死循环
        Iterator<KnowledgeDocument> iterator = documents.iterator();
        while (iterator.hasNext()) {
            KnowledgeDocument doc = iterator.next();
            QueryWrapper<KnowledgeSegment> segWrapper = new QueryWrapper<>();
            segWrapper.eq("document_id", doc.getDocId())
                    .ne("document_version", doc.getCurrentVersionId());
            long staleCount = knowledgeSegmentService.count(segWrapper);
            if (staleCount == 0) {
                iterator.remove();
            }
        }

        return documents;
    }
}