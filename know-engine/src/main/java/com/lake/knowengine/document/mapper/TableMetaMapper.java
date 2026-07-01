package com.lake.knowengine.document.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lake.knowengine.document.entity.TableMeta;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TableMetaMapper extends BaseMapper<TableMeta> {

    /**
     * 删除动态创建的物理表。
     * 调用前必须完成表名合法性校验。
     */
    @Delete("DROP TABLE IF EXISTS `${tableName}`")
    void dropTable(@Param("tableName") String tableName);

    /**
     * 按表名物理删除元数据记录，绕过 @TableLogic。
     */
    @Delete("DELETE FROM table_meta WHERE table_name = #{tableName}")
    int physicalDeleteByTableName(@Param("tableName") String tableName);
}
