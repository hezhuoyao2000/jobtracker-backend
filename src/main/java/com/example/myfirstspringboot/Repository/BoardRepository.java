package com.example.myfirstspringboot.Repository;

import com.example.myfirstspringboot.Entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, UUID> {
    //  根据用户 ID 查询所有看板，按创建时间升序
    List<Board> findByUserIdOrderByCreatedAtAsc(String userId);

    Optional<Board> findByIdAndUserId(UUID id, String userId);

}
