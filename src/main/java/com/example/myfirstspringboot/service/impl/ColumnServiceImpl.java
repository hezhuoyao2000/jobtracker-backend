package com.example.myfirstspringboot.service.impl;

import com.example.myfirstspringboot.Entity.KanbanColumn;
import com.example.myfirstspringboot.dto.request.UpdateColumnRequest;
import com.example.myfirstspringboot.dto.response.ColumnDto;
import com.example.myfirstspringboot.exception.BusinessException;
import com.example.myfirstspringboot.exception.ResourceNotFoundException;
import com.example.myfirstspringboot.exception.UnauthorizedException;
import com.example.myfirstspringboot.mapper.BoardMapper;
import com.example.myfirstspringboot.mapper.KanbanColumnMapper;
import com.example.myfirstspringboot.service.ColumnService;
import com.example.myfirstspringboot.util.DtoConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 列服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColumnServiceImpl implements ColumnService {

    private final KanbanColumnMapper columnMapper;
    private final BoardMapper boardMapper;
    private final DtoConverter dtoConverter;
    private final ObjectMapper objectMapper;

    /**
     * 更新列信息
     * @param userId 当前用户 ID
     * @param request 更新列请求参数
     * @return 更新后的列信息
     */
    @Override
    @Transactional
    public ColumnDto updateColumn(String userId, UpdateColumnRequest request) {
        UUID columnId = request.getColumnId();
        log.info("更新列信息: columnId={}, userId={}", columnId, userId);

        // 步骤 1: 查询列
        KanbanColumn column = columnMapper.selectById(columnId);
        if (column == null) {
            log.warn("列不存在: columnId={}", columnId);
            throw new ResourceNotFoundException("列", "id", columnId);
        }

        // 步骤 2: 校验看板属于当前用户
        UUID boardId = column.getBoardId();
        boolean boardExists = boardMapper.existsByIdAndUserId(boardId, userId);
        if (!boardExists) {
            log.warn("无权更新列: columnId={}, boardId={}, userId={}", columnId, boardId, userId);
            throw new UnauthorizedException("无权更新该列");
        }

        // 步骤 3: 只更新传入的非空字段
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            column.setName(request.getName());
            log.debug("更新列名称: {}", request.getName());
        }

        if (request.getSortOrder() != null) {
            column.setSortOrder(request.getSortOrder());
            log.debug("更新列排序: {}", request.getSortOrder());
        }

        if (request.getCustomAttributes() != null) {
            try {
                String customAttributesJson = objectMapper.writeValueAsString(request.getCustomAttributes());
                column.setCustomAttributes(customAttributesJson);
                log.debug("更新列自定义属性");
            } catch (JsonProcessingException e) {
                log.error("自定义属性序列化失败", e);
                throw new BusinessException("自定义属性序列化失败");
            }
        }

        // 步骤 4: 保存到数据库
        columnMapper.updateById(column);
        log.info("列更新成功: columnId={}", columnId);

        // 步骤 5: 转换为 DTO 并返回
        return dtoConverter.toColumnDto(column);
    }
}
