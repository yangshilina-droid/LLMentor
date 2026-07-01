package com.lake.knowengine.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lake.knowengine.document.entity.KnowledgeSegment;
import com.lake.knowengine.document.service.KnowledgeSegmentService;
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
@RequestMapping("/knowledge/segments")
@RequiredArgsConstructor
public class KnowledgeSegmentController {

    private final KnowledgeSegmentService knowledgeSegmentService;

    @PostMapping
    public Boolean create(@RequestBody KnowledgeSegment segment) {
        return knowledgeSegmentService.save(segment);
    }

    @GetMapping("/{id}")
    public KnowledgeSegment getById(@PathVariable Long id) {
        return knowledgeSegmentService.getById(id);
    }

    @GetMapping
    public IPage<KnowledgeSegment> page(@RequestParam(defaultValue = "1") Long current,
                                        @RequestParam(defaultValue = "10") Long size,
                                        @RequestParam(required = false) Long documentId,
                                        @RequestParam(required = false) Long documentVersion,
                                        @RequestParam(required = false) String status,
                                        @RequestParam(required = false) Integer skipEmbedding) {
        LambdaQueryWrapper<KnowledgeSegment> queryWrapper = new LambdaQueryWrapper<KnowledgeSegment>()
                .eq(documentId != null, KnowledgeSegment::getDocumentId, documentId)
                .eq(documentVersion != null, KnowledgeSegment::getDocumentVersion, documentVersion)
                .eq(StringUtils.hasText(status), KnowledgeSegment::getStatus, status)
                .eq(skipEmbedding != null, KnowledgeSegment::getSkipEmbedding, skipEmbedding)
                .orderByAsc(KnowledgeSegment::getDocumentId)
                .orderByAsc(KnowledgeSegment::getChunkOrder)
                .orderByAsc(KnowledgeSegment::getId);
        return knowledgeSegmentService.page(Page.of(current, size), queryWrapper);
    }

    @PutMapping("/{id}")
    public Boolean update(@PathVariable Long id, @RequestBody KnowledgeSegment segment) {
        segment.setId(id);
        return knowledgeSegmentService.updateById(segment);
    }

    @DeleteMapping("/{id}")
    public Boolean delete(@PathVariable Long id) {
        return knowledgeSegmentService.removeById(id);
    }
}
