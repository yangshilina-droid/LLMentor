package com.lake.knowengine.document.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowengine.document.constant.DocumentStatus;
import com.lake.knowengine.document.entity.KnowledgeDocumentVersion;
import com.lake.knowengine.document.mapper.KnowledgeDocumentVersionMapper;
import com.lake.knowengine.document.service.KnowledgeDocumentVersionService;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class KnowledgeDocumentVersionServiceImpl
        extends ServiceImpl<KnowledgeDocumentVersionMapper, KnowledgeDocumentVersion>
        implements KnowledgeDocumentVersionService {

    @Override
    public boolean existsByContentHash(String contentHash) {
        return count(new QueryWrapper<KnowledgeDocumentVersion>()
                .eq("content_hash", contentHash)) > 0;
    }

    @Override
    public KnowledgeDocumentVersion createVersionRecord(Long docId,
                                                        String version,
                                                        String docUrl,
                                                        String convertedDocUrl,
                                                        String uploadUser,
                                                        String contentHash,
                                                        DocumentStatus status,
                                                        String changelog) {
        KnowledgeDocumentVersion documentVersion = new KnowledgeDocumentVersion();
        documentVersion.setDocId(docId);
        documentVersion.setVersion(StringUtils.hasText(version) ? version : "1.0.0");
        documentVersion.setDocUrl(docUrl);
        documentVersion.setConvertedDocUrl(convertedDocUrl);
        documentVersion.setUploadUser(uploadUser);
        documentVersion.setContentHash(contentHash);
        documentVersion.setStatus(status.name());
        documentVersion.setChangelog(changelog);
        save(documentVersion);
        return documentVersion;
    }
}
