package com.example.myfirstspringboot.service;

import com.example.myfirstspringboot.service.BoardService;
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

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * Board 服务实现类
 */
@Service
@RequiredArgsConstructor  // Lombok 注解，自动生成构造器
public class BoardServiceImpl implements BoardService {

    // ========== 依赖注入 ==========

    private final BoardRepository boardRepository;           // JPA Repository
    private final KanbanColumnRepository columnRepository;   // JPA Repository
    private final BoardMapper boardMapper;                   // MyBatis Mapper
    private final DtoConverter dtoConverter;                 // DTO 转换工具

    // ========== 方法实现 ==========

    @Override
    public BoardDataDto loadBoard(String userId, LoadBoardRequest request) {
        // TODO: 下节课详细讲解
        return null;
    }

    @Override
    @Transactional  // 事务注解，保证创建看板和创建列要么都成功，要么都失败
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
    private void createDefaultColumns(java.util.UUID boardId) {
        // 默认的 5 个列名
        List<String> defaultColumnNames = Arrays.asList(
                "Wish list",
                "Applied",
                "Interviewing",
                "Offered",
                "Rejected"
        );

        // 批量创建列
        for (int i = 0; i < defaultColumnNames.size(); i++) {
            KanbanColumn column = new KanbanColumn();
            column.setBoardId(boardId);
            column.setName(defaultColumnNames.get(i));
            column.setSortOrder(i);  // 排序：0, 1, 2, 3, 4
            column.setIsDefault(true);

            // 保存到数据库
            columnRepository.save(column);
        }
    }
}
