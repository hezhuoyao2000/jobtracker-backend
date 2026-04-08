package com.example.myfirstspringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.myfirstspringboot.Entity.KanbanColumn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface KanbanColumnMapper extends BaseMapper<KanbanColumn> {

    /**
     * 根据看板 ID 查询所有列，按 sortOrder 升序
     */
    List<KanbanColumn> selectByBoardIdOrderBySortOrderAsc(@Param("boardId") UUID boardId);

    /**
     * 检查某个列是否属于某个看板
     */
    boolean existsByIdAndBoardId(@Param("columnId") UUID columnId, @Param("boardId") UUID boardId);
}
