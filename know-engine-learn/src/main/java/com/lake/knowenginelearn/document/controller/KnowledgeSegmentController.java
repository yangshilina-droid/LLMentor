package com.lake.knowenginelearn.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lake.knowenginelearn.document.entity.KnowledgeSegment;
import com.lake.knowenginelearn.document.service.KnowledgeSegmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/knowledge-segments")
public class KnowledgeSegmentController {

    private final KnowledgeSegmentService knowledgeSegmentService;

    @PostMapping
    public boolean create(@RequestBody KnowledgeSegment knowledgeSegment) {
        return knowledgeSegmentService.save(knowledgeSegment);
    }

    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return knowledgeSegmentService.removeById(id);
    }

    @PutMapping("/{id}")
    public boolean update(@PathVariable Long id, @RequestBody KnowledgeSegment knowledgeSegment) {
        knowledgeSegment.setId(id);
        return knowledgeSegmentService.updateById(knowledgeSegment);
    }

    @GetMapping("/{id}")
    public KnowledgeSegment get(@PathVariable Long id) {
        return knowledgeSegmentService.getById(id);
    }

    @GetMapping
    public List<KnowledgeSegment> list(@RequestParam(required = false) Long documentId) {
        LambdaQueryWrapper<KnowledgeSegment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(documentId != null, KnowledgeSegment::getDocumentId, documentId)
                .orderByAsc(KnowledgeSegment::getChunkOrder);
        return knowledgeSegmentService.list(queryWrapper);
    }

    @GetMapping("/page")
    public Page<KnowledgeSegment> page(@RequestParam(defaultValue = "1") long current,
                                       @RequestParam(defaultValue = "10") long size,
                                       @RequestParam(required = false) Long documentId) {
        LambdaQueryWrapper<KnowledgeSegment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(documentId != null, KnowledgeSegment::getDocumentId, documentId)
                .orderByAsc(KnowledgeSegment::getChunkOrder);
        return knowledgeSegmentService.page(Page.of(current, size), queryWrapper);
    }
}
