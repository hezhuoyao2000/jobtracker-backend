package com.example.myfirstspringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.myfirstspringboot.Entity.Board;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface BoardMapper extends BaseMapper<Board> {

    /**
     * 根据用户 ID 查询所有看板，按创建时间升序
     */
    List<Board> selectByUserIdOrderByCreatedAtAsc(@Param("userId") String userId);

    /**
     * 根据看板 ID 和用户 ID 查询看板
     */
    Board selectByIdAndUserId(@Param("id") UUID id, @Param("userId") String userId);

    /**
     * 检查看板是否存在且属于指定用户
     */
    boolean existsByIdAndUserId(@Param("id") UUID id, @Param("userId") String userId);

    /**
     * 根据用户 ID 查询第一个看板（按创建时间升序）
     */
    Board selectFirstByUserIdOrderByCreatedAtAsc(@Param("userId") String userId);
}
