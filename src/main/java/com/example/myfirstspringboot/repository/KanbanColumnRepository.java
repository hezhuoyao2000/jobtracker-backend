package com.example.myfirstspringboot.repository;

import com.example.myfirstspringboot.Entity.KanbanColumn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface KanbanColumnRepository extends JpaRepository<KanbanColumn, UUID> {

    // 根据看板 ID 查询所有列，按 sortOrder 升序
    List<KanbanColumn> findByBoardIdOrderBySortOrderAsc(UUID boardId);

    // 检查某个列是否属于某个看板
    boolean existsByIdAndBoardId(UUID columnId, UUID boardId);

}