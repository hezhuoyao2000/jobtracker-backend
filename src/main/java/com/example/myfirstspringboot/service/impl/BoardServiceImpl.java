package com.example.myfirstspringboot.service.impl;

import com.example.myfirstspringboot.Entity.Board;
import com.example.myfirstspringboot.Entity.JobCard;
import com.example.myfirstspringboot.Entity.KanbanColumn;
import com.example.myfirstspringboot.dto.request.CreateBoardRequest;
import com.example.myfirstspringboot.dto.request.LoadBoardRequest;
import com.example.myfirstspringboot.dto.response.BoardDataDto;
import com.example.myfirstspringboot.dto.response.BoardDto;
import com.example.myfirstspringboot.repository.BoardRepository;
import com.example.myfirstspringboot.repository.JobCardRepository;
import com.example.myfirstspringboot.repository.KanbanColumnRepository;
import com.example.myfirstspringboot.service.BoardService;
import com.example.myfirstspringboot.util.DtoConverter;
import com.example.myfirstspringboot.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Board 服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BoardServiceImpl implements BoardService {

    private final BoardRepository boardRepository;
    private final KanbanColumnRepository columnRepository;
    private final JobCardRepository jobCardRepository;
    private final DtoConverter dtoConverter;

    // ========== 方法实现 ==========

    /**
     * 1、创建看板
     * @param userId 用户 ID
     * @param request 请求参数（包含看板名称）
     * @return 创建后的看板信息
     */
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
     * 2、加载看板完整数据
     * @param userId 当前用户 ID
     * @param request 请求参数（包含可选的 boardId）
     * @return 看板完整数据（board + columns + cards）
     */
    @Override
    @Transactional(readOnly = true)  // 只读事务，性能优化
    public BoardDataDto loadBoard(String userId, LoadBoardRequest request) {
        UUID boardId = request.getBoardId();

        Board board;
        if (boardId == null) {
            // 场景 A: 没有指定 boardId，加载用户的第一个看板
            board = boardRepository.findFirstByUserIdOrderByCreatedAtAsc(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("没有找到该用户的看板"));
        } else {
            // 场景 B: 指定了 boardId，加载该看板并校验权限
            board = boardRepository.findByIdAndUserId(boardId, userId)
                    .orElseThrow(() -> new ResourceNotFoundException("看板不存在或不属于该用户"));
        }

        // 查询该看板的所有列（按 sortOrder 排序）
        List<KanbanColumn> columns = columnRepository.findByBoardIdOrderBySortOrderAsc(board.getId());

        // 查询该看板的所有卡片（未删除的）
        List<JobCard> cards = jobCardRepository.findByBoardIdAndDeletedAtIsNull(board.getId());

        // 组装 BoardDataDto 并返回
        return dtoConverter.toBoardDataDto(board, columns, cards);
    }

    // ========== 辅助方法 ==========

    /**
     * 用于 1、创建看板
     * 为新建的看板创建默认的 5 个列
     * @param boardId 看板 ID
     */
    private void createDefaultColumns(UUID boardId) {
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
