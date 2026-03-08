package com.example.myfirstspringboot.repository;

import com.example.myfirstspringboot.Entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, UUID> {
    //  根据用户 ID 查询所有看板，按创建时间升序
    List<Board> findByUserIdOrderByCreatedAtAsc(String userId);

    // 根据用户 ID 查询第一个看板（按创建时间升序）
    Optional<Board> findFirstByUserIdOrderByCreatedAtAsc(String userId);

    Optional<Board> findByIdAndUserId(UUID id, String userId);

    // 检查看板是否存在且属于指定用户
    boolean existsByIdAndUserId(UUID id, String userId);

}
