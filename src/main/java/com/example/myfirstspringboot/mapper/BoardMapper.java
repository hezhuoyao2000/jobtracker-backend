package com.example.myfirstspringboot.mapper;

import com.example.myfirstspringboot.dto.response.BoardDataDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.UUID;

/**
 * Board MyBatis Mapper
 * 用于复杂查询和联表操作
 */
@Mapper
public interface BoardMapper {

    /**
     * 根据用户 ID 加载看板完整数据（board + columns + cards）
     * @param userId 用户 ID
     * @return 看板完整数据
     */
    BoardDataDto findBoardDataByUserId(@Param("userId") String userId);

    /**
     * 根据看板 ID 加载看板完整数据（board + columns + cards）
     * @param boardId 看板 ID
     * @param userId 用户 ID（用于权限校验）
     * @return 看板完整数据
     */
    BoardDataDto findBoardDataByBoardId(
        @Param("boardId") UUID boardId,
        @Param("userId") String userId
    );
}
