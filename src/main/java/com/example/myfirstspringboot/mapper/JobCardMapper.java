package com.example.myfirstspringboot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.myfirstspringboot.Entity.JobCard;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.UUID;

@Mapper
public interface JobCardMapper extends BaseMapper<JobCard> {

    /**
     * 根据看板 ID 查询所有未删除的卡片
     */
    List<JobCard> selectByBoardIdAndDeletedAtIsNull(@Param("boardId") UUID boardId);

    /**
     * 根据 ID 查询未删除的卡片
     */
    JobCard selectByIdAndDeletedAtIsNull(@Param("id") UUID id);

    /**
     * 根据看板 ID 查询所有卡片（包括已删除）
     */
    List<JobCard> selectByBoardId(@Param("boardId") UUID boardId);
}
