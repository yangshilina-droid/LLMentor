package com.lake.knowengine.document.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lake.knowengine.document.entity.TableMeta;
import com.lake.knowengine.document.service.TableMetaService;
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
@RequestMapping("/knowledge/table-metas")
@RequiredArgsConstructor
public class TableMetaController {

    private final TableMetaService tableMetaService;

    @PostMapping
    public Boolean create(@RequestBody TableMeta tableMeta) {
        return tableMetaService.save(tableMeta);
    }

    @GetMapping("/{id}")
    public TableMeta getById(@PathVariable Long id) {
        return tableMetaService.getById(id);
    }

    @GetMapping
    public IPage<TableMeta> page(@RequestParam(defaultValue = "1") Long current,
                                 @RequestParam(defaultValue = "10") Long size,
                                 @RequestParam(required = false) String tableName,
                                 @RequestParam(required = false) Long versionId) {
        LambdaQueryWrapper<TableMeta> queryWrapper = new LambdaQueryWrapper<TableMeta>()
                .eq(StringUtils.hasText(tableName), TableMeta::getTableName, tableName)
                .eq(versionId != null, TableMeta::getVersionId, versionId)
                .orderByDesc(TableMeta::getCreatedAt)
                .orderByDesc(TableMeta::getId);
        return tableMetaService.page(Page.of(current, size), queryWrapper);
    }

    @PutMapping("/{id}")
    public Boolean update(@PathVariable Long id, @RequestBody TableMeta tableMeta) {
        tableMeta.setId(id);
        return tableMetaService.updateById(tableMeta);
    }

    @DeleteMapping("/{id}")
    public Boolean delete(@PathVariable Long id) {
        return tableMetaService.removeById(id);
    }
}
