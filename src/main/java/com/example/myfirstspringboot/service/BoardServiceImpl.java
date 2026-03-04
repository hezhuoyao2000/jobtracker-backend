package com.example.myfirstspringboot.service;

import com.example.myfirstspringboot.Entity.JobCard;
import com.example.myfirstspringboot.repository.JobCardRepository;
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
import com.example.myfirstspringboot.util.DtoConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Board 服务实现类
 */
@Service
@RequiredArgsConstructor  // Lombok 注解，自动生成构造器
public class BoardServiceImpl implements BoardService {

    // ========== 依赖注入 ==========

    private final BoardRepository boardRepository;           // JPA Repository
    private final KanbanColumnRepository columnRepository;   // JPA Repository
    private final JobCardRepository jobCardRepository;
    private final BoardMapper boardMapper;                   // MyBatis Mapper
    private final DtoConverter dtoConverter;                 // DTO 转换工具

    // ========== 方法实现 ==========

    // 1、 创建看板
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

    // 1、创建看板 辅助方法
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

    // 2、加载看板 JPA方案
    @Override
    public BoardDataDto loadBoard(String userId, LoadBoardRequest request) {

        //  ========== 步骤 1: 确定要加载哪个看板 ==========
        UUID boardId = request.getBoardId();
        Board board;

        if (boardId == null) {
            // 场景 A: 没有指定 boardId，加载用户的第一个看板
            board = loadUserFirstBoard(userId);
        } else {
            // 场景 B: 指定了 boardId，加载该看板并校验权限
            board = loadBoardAndValidatePermission(boardId, userId);
        }

        // 如果看板不存在，抛出异常
        if (board == null) {
            throw new RuntimeException("看板不存在");
        }

        // ========== 步骤 2: 查询该看板的所有列 ==========

        List<KanbanColumn> columns = columnRepository.findByBoardIdOrderBySortOrderAsc(board.getId());

        // ========== 步骤 3: 查询该看板的所有未删除卡片 ==========

        List<JobCard> cards = jobCardRepository.findByBoardIdAndDeletedAtIsNull(board.getId());

        // ========== 步骤 4: 组装 DTO 并返回 ==========

        return assembleBoardDataDto(board, columns, cards);

    }

    // 2. 加载看板服务 辅助方法
    /**
     * 加载用户的第一个看板
     * @param userId 用户 ID
     * @return 用户的第一个看板（按创建时间升序）
     */
    private Board loadUserFirstBoard(String userId) {
        List<Board> boards = boardRepository.findByUserIdOrderByCreatedAtAsc(userId);

        // 如果用户没有任何看板，返回 null
        if (boards == null || boards.isEmpty()) {
            return null;
        }

        // 返回第一个看板
        return boards.get(0);
    }

    /**
     * 加载看板并校验权限
     * @param boardId 看板 ID
     * @param userId 用户 ID
     * @return 看板实体
     * @throws RuntimeException 如果看板不存在或不属于该用户
     */
    private Board loadBoardAndValidatePermission(UUID boardId, String userId) {
        Optional<Board> boardOpt = boardRepository.findByIdAndUserId(boardId, userId);

        // 如果看板不存在或不属于该用户，抛出异常
        if (!boardOpt.isPresent()) {
            throw new RuntimeException("看板不存在或无权访问");
        }

        return boardOpt.get();
    }

    /**
     * 组装 BoardDataDto
     */
    private BoardDataDto assembleBoardDataDto(Board board,
                                              List<KanbanColumn> columns,
                                              List<JobCard> cards) {
        BoardDataDto boardDataDto = new BoardDataDto();
        boardDataDto.setBoard(dtoConverter.toBoardDto(board));
        boardDataDto.setColumns(dtoConverter.toColumnDtoList(columns));
        boardDataDto.setCards(dtoConverter.toJobCardDtoList(cards));
        return boardDataDto;
    }


}
