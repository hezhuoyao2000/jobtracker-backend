package com.example.myfirstspringboot.repository;

import com.example.myfirstspringboot.Entity.JobCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface JobCardRepository extends JpaRepository<JobCard, UUID> {
    // 根据看板 ID 查询所有未删除的卡片
    List<JobCard> findByBoardIdAndDeletedAtIsNull(UUID boardId);

    // 根据 ID 查询未删除的卡片
    Optional<JobCard> findByIdAndDeletedAtIsNull(UUID id);

    // 根据看板 ID 查询（用于权限校验）
    List<JobCard> findByBoardId(UUID boardId);

}
