package com.lake.knowengine.document.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.lake.knowengine.document.entity.TableMeta;
import com.lake.knowengine.document.mapper.TableMetaMapper;
import com.lake.knowengine.document.service.TableMetaService;
import org.springframework.stereotype.Service;

@Service
public class TableMetaServiceImpl extends ServiceImpl<TableMetaMapper, TableMeta>
        implements TableMetaService {
}
