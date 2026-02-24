package com.example.myfirstspringboot.service.impl;

import com.example.myfirstspringboot.Entity.Board;
import com.example.myfirstspringboot.Entity.KanbanColumn;
import com.example.myfirstspringboot.dto.request.CreateBoardRequest;
import com.example.myfirstspringboot.dto.request.LoadBoardRequest;
import com.example.myfirstspringboot.dto.response.BoardDataDto;
import com.example.myfirstspringboot.dto.response.BoardDto;
import com.example.myfirstspringboot.mapper.BoardMapper;
import com.example.myfirstspringboot.repository.BoardRepository;
import com.example.myfirstspringboot.repository.KanbanColumnRepository;
import com.example.myfirstspringboot.service.BoardService;
import com.example.myfirstspringboot.util.DtoConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Board 服务实现类
 */
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final KanbanColumnRepository columnRepository;
    private final BoardMapper boardMapper;
    private final DtoConverter dtoConverter;

    @Override
    public BoardDataDto loadBoard(String userId, LoadBoardRequest request) {
        // TODO: 后续实现
        return null;
    }

    @Override
    @Transactional
    public BoardDto createBoard(String userId, CreateBoardRequest request) {
        // 步骤 1: 创建 Board 实体
        Board board = new Board();
        board.setUserId(userId);

        // 步骤 2: 设置看板名称（如果为空，使用默认名称）
        String boardName = request.getName();
        if (boardName == null || boardName.trim().isEmpty()) {
            boardName = "My Job Tracker";
        }
        board.setName(boardName);

        // 步骤 3: 保存到数据库
        Board savedBoard = boardRepository.save(board);

        // 步骤 4: 创建默认的 5 个列
        createDefaultColumns(savedBoard.getId());

        // 步骤 5: 转换为 DTO 并返回
        return dtoConverter.toBoardDto(savedBoard);
    }

    /**
     * 为新建的看板创建默认的 5 个列
     * @param boardId 看板 ID
     */
    private void createDefaultColumns(UUID boardId) {
        List<String> defaultColumnNames = Arrays.asList(
            "Wish list",
            "Applied",
            "Interviewing",
            "Offered",
            "Rejected"
        );

        for (int i = 0; i < defaultColumnNames.size(); i++) {
            KanbanColumn column = new KanbanColumn();
            column.setBoardId(boardId);
            column.setName(defaultColumnNames.get(i));
            column.setSortOrder(i);
            column.setIsDefault(true);
            columnRepository.save(column);
        }
    }
}
