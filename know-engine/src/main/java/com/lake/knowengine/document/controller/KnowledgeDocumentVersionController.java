package com.lake.knowengine.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lake.knowengine.document.entity.KnowledgeDocumentVersion;
import com.lake.knowengine.document.service.KnowledgeDocumentVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/knowledge/document-versions")
@RequiredArgsConstructor
public class KnowledgeDocumentVersionController {

    private final KnowledgeDocumentVersionService knowledgeDocumentVersionService;

    @PostMapping
    public Boolean create(@RequestBody KnowledgeDocumentVersion documentVersion) {
        return knowledgeDocumentVersionService.save(documentVersion);
    }

    @GetMapping("/{versionId}")
    public KnowledgeDocumentVersion getById(@PathVariable Long versionId) {
        return knowledgeDocumentVersionService.getById(versionId);
    }

    @GetMapping
    public IPage<KnowledgeDocumentVersion> page(@RequestParam(defaultValue = "1") Long current,
                                                @RequestParam(defaultValue = "10") Long size,
                                                @RequestParam(required = false) Long docId,
                                                @RequestParam(required = false) String version,
                                                @RequestParam(required = false) String status,
                                                @RequestParam(required = false) String contentHash) {
        LambdaQueryWrapper<KnowledgeDocumentVersion> queryWrapper =
                new LambdaQueryWrapper<KnowledgeDocumentVersion>()
                        .eq(docId != null, KnowledgeDocumentVersion::getDocId, docId)
                        .eq(StringUtils.hasText(version), KnowledgeDocumentVersion::getVersion, version)
                        .eq(StringUtils.hasText(status), KnowledgeDocumentVersion::getStatus, status)
                        .eq(StringUtils.hasText(contentHash), KnowledgeDocumentVersion::getContentHash, contentHash)
                        .orderByDesc(KnowledgeDocumentVersion::getCreatedAt)
                        .orderByDesc(KnowledgeDocumentVersion::getVersionId);
        return knowledgeDocumentVersionService.page(Page.of(current, size), queryWrapper);
    }

    @PutMapping("/{versionId}")
    public Boolean update(@PathVariable Long versionId,
                          @RequestBody KnowledgeDocumentVersion documentVersion) {
        documentVersion.setVersionId(versionId);
        return knowledgeDocumentVersionService.updateById(documentVersion);
    }

    @DeleteMapping("/{versionId}")
    public Boolean delete(@PathVariable Long versionId) {
        return knowledgeDocumentVersionService.removeById(versionId);
    }
}
