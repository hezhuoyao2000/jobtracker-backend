package com.example.myfirstspringboot.service.impl;

import com.example.myfirstspringboot.Entity.KanbanColumn;
import com.example.myfirstspringboot.dto.request.UpdateColumnRequest;
import com.example.myfirstspringboot.dto.response.ColumnDto;
import com.example.myfirstspringboot.repository.BoardRepository;
import com.example.myfirstspringboot.repository.KanbanColumnRepository;
import com.example.myfirstspringboot.service.ColumnService;
import com.example.myfirstspringboot.util.DtoConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 列服务实现类
 */
@Service
@RequiredArgsConstructor
public class ColumnServiceImpl implements ColumnService {

    private final KanbanColumnRepository columnRepository;
    private final BoardRepository boardRepository;
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

        // 步骤 1: 查询列
        KanbanColumn column = columnRepository.findById(columnId)
                .orElseThrow(() -> new RuntimeException("列不存在"));

        // 步骤 2: 校验看板属于当前用户
        UUID boardId = column.getBoardId();
        boolean boardExists = boardRepository.existsByIdAndUserId(boardId, userId);
        if (!boardExists) {
            throw new RuntimeException("看板不存在或不属于该用户");
        }

        // 步骤 3: 只更新传入的非空字段
        if (request.getName() != null && !request.getName().trim().isEmpty()) {
            column.setName(request.getName());
        }

        if (request.getSortOrder() != null) {
            column.setSortOrder(request.getSortOrder());
        }

        if (request.getCustomAttributes() != null) {
            try {
                String customAttributesJson = objectMapper.writeValueAsString(request.getCustomAttributes());
                column.setCustomAttributes(customAttributesJson);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("自定义属性序列化失败", e);
            }
        }

        // 步骤 4: 保存到数据库
        KanbanColumn savedColumn = columnRepository.save(column);

        // 步骤 5: 转换为 DTO 并返回
        return dtoConverter.toColumnDto(savedColumn);
    }
}
