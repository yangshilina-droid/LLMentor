package com.lake.knowenginelearn.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowenginelearn.document.entity.KnowledgeDocumentVersion;
import com.lake.knowenginelearn.document.mapper.KnowledgeDocumentVersionMapper;
import com.lake.knowenginelearn.document.service.KnowledgeDocumentVersionService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 文档版本表 Service 实现类
 */
@Service
public class KnowledgeDocumentVersionServiceImpl
        extends ServiceImpl<KnowledgeDocumentVersionMapper, KnowledgeDocumentVersion>
        implements KnowledgeDocumentVersionService {

    /**
     * 语义化版本比较器（按 major.minor.patch 数值比较）
     */
    private static final Comparator<KnowledgeDocumentVersion> VERSION_COMPARATOR =
            Comparator.comparing(KnowledgeDocumentVersion::getVersion, KnowledgeDocumentVersionServiceImpl::compareVersions);

    @Override
    public List<KnowledgeDocumentVersion> listByDocId(Long docId) {
        List<KnowledgeDocumentVersion> versions = list(new QueryWrapper<KnowledgeDocumentVersion>()
                .eq("doc_id", docId));
        // 在 Java 层按语义版本降序排序
        versions.sort(VERSION_COMPARATOR.reversed());
        return versions;
    }

    @Override
    public List<KnowledgeDocumentVersion> listByDocIdAndVersion(Long docId, String version) {
        return list(new QueryWrapper<KnowledgeDocumentVersion>()
                .eq("doc_id", docId)
                .eq("version", version));
    }

    @Override
    public String getLatestVersion(Long docId) {
        List<KnowledgeDocumentVersion> versions = listByDocId(docId);
        if (versions.isEmpty()) {
            return null;
        }
        return versions.get(0).getVersion();
    }

    @Override
    public boolean existsByContentHash(String contentHash) {
        return count(new QueryWrapper<KnowledgeDocumentVersion>()
                .eq("content_hash", contentHash)) > 0;
    }

    /**
     * 语义化版本号比较（如 "1.0.0" vs "2.1.3"）
     *
     * @return 负数表示 v1 < v2，0 表示相等，正数表示 v1 > v2
     */
    static int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int p1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
            int p2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;
            if (p1 != p2) return Integer.compare(p1, p2);
        }
        return 0;
    }
}
