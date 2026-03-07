package com.example.myfirstspringboot.service.impl;

import com.example.myfirstspringboot.Entity.Board;
import com.example.myfirstspringboot.Entity.JobCard;
import com.example.myfirstspringboot.Entity.KanbanColumn;
import com.example.myfirstspringboot.dto.request.CreateBoardRequest;
import com.example.myfirstspringboot.dto.request.LoadBoardRequest;
import com.example.myfirstspringboot.dto.response.BoardDataDto;
import com.example.myfirstspringboot.dto.response.BoardDto;
import com.example.myfirstspringboot.dto.response.ColumnDto;
import com.example.myfirstspringboot.dto.response.JobCardDto;
import com.example.myfirstspringboot.repository.BoardRepository;
import com.example.myfirstspringboot.repository.JobCardRepository;
import com.example.myfirstspringboot.repository.KanbanColumnRepository;
import com.example.myfirstspringboot.util.DtoConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BoardServiceImpl 单元测试类
 *
 * 测试说明：
 * - 使用 Mockito 框架进行单元测试
 * - 不依赖真实数据库，使用 Mock 对象模拟 Repository
 * - 全部使用 JPA 方案（Repository + DtoConverter）
 */
@ExtendWith(MockitoExtension.class)
class BoardServiceImplTest {

    // ========== Mock 对象 ==========
    @Mock
    private BoardRepository boardRepository;

    @Mock
    private KanbanColumnRepository columnRepository;

    @Mock
    private JobCardRepository jobCardRepository;

    @Mock
    private DtoConverter dtoConverter;

    // ========== 被测试对象 ==========
    @InjectMocks
    private BoardServiceImpl boardService;

    // ========== 测试数据 ==========
    private UUID testBoardId;
    private String testUserId;
    private CreateBoardRequest createBoardRequest;
    private Board testBoard;
    private BoardDto testBoardDto;

    @BeforeEach
    void setUp() {
        testBoardId = UUID.randomUUID();
        testUserId = "test-user-123";

        createBoardRequest = new CreateBoardRequest();
        createBoardRequest.setName("Test Board");

        testBoard = new Board();
        testBoard.setId(testBoardId);
        testBoard.setUserId(testUserId);
        testBoard.setName("Test Board");

        testBoardDto = new BoardDto();
        testBoardDto.setId(testBoardId);
        testBoardDto.setUserId(testUserId);
        testBoardDto.setName("Test Board");
    }

    // ============ 创建看板测试 ============ //

    @Test
    @DisplayName("测试创建看板 - 指定名称")
    void testCreateBoard_WithName() {
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        when(dtoConverter.toBoardDto(testBoard)).thenReturn(testBoardDto);

        BoardDto result = boardService.createBoard(testUserId, createBoardRequest);

        assertNotNull(result);
        assertEquals(testBoardId, result.getId());
        assertEquals(testUserId, result.getUserId());
        assertEquals("Test Board", result.getName());

        verify(boardRepository, times(1)).save(any(Board.class));
        verify(dtoConverter, times(1)).toBoardDto(testBoard);
        verify(columnRepository, times(5)).save(any(KanbanColumn.class));
    }

    @Test
    @DisplayName("测试创建看板 - 名称为空时使用默认名称")
    void testCreateBoard_WithEmptyName_UsesDefaultName() {
        createBoardRequest.setName(null);

        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        when(dtoConverter.toBoardDto(any(Board.class))).thenReturn(testBoardDto);

        BoardDto result = boardService.createBoard(testUserId, createBoardRequest);

        assertNotNull(result);

        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepository, times(1)).save(boardCaptor.capture());

        Board savedBoard = boardCaptor.getValue();
        assertEquals("My Job Tracker", savedBoard.getName());
    }

    @Test
    @DisplayName("测试创建看板 - 名称为纯空格时使用默认名称")
    void testCreateBoard_WithBlankName_UsesDefaultName() {
        createBoardRequest.setName("   ");

        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        when(dtoConverter.toBoardDto(any(Board.class))).thenReturn(testBoardDto);

        BoardDto result = boardService.createBoard(testUserId, createBoardRequest);

        assertNotNull(result);

        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepository, times(1)).save(boardCaptor.capture());

        Board savedBoard = boardCaptor.getValue();
        assertEquals("My Job Tracker", savedBoard.getName());
    }

    @Test
    @DisplayName("测试创建看板 - 验证默认列的创建")
    void testCreateBoard_VerifyDefaultColumns() {
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        when(dtoConverter.toBoardDto(any(Board.class))).thenReturn(testBoardDto);

        boardService.createBoard(testUserId, createBoardRequest);

        ArgumentCaptor<KanbanColumn> columnCaptor = ArgumentCaptor.forClass(KanbanColumn.class);
        verify(columnRepository, times(5)).save(columnCaptor.capture());

        List<KanbanColumn> savedColumns = columnCaptor.getAllValues();
        assertEquals(5, savedColumns.size());

        List<String> expectedColumnNames = Arrays.asList(
            "Wish list", "Applied", "Interviewing", "Offered", "Rejected"
        );

        for (int i = 0; i < savedColumns.size(); i++) {
            KanbanColumn column = savedColumns.get(i);
            assertEquals(expectedColumnNames.get(i), column.getName());
            assertEquals(testBoardId, column.getBoardId());
            assertEquals(i, column.getSortOrder());
            assertTrue(column.getIsDefault());
        }
    }

    // ============ 加载看板测试 ============ //
    // JPA 方案：3 次查询 + DtoConverter 组装

    @Test
    @DisplayName("测试加载看板 - 指定 boardId 成功")
    void testLoadBoard_WithBoardId_Success() {
        UUID boardId = UUID.randomUUID();
        String userId = "test-user-123";

        LoadBoardRequest request = new LoadBoardRequest();
        request.setBoardId(boardId);

        // 准备实体数据
        Board board = new Board();
        board.setId(boardId);
        board.setUserId(userId);
        board.setName("Test Board");

        List<KanbanColumn> columns = new ArrayList<>();
        KanbanColumn col1 = new KanbanColumn();
        col1.setId(UUID.randomUUID());
        col1.setName("Wish list");
        columns.add(col1);

        List<JobCard> cards = new ArrayList<>();
        JobCard card1 = new JobCard();
        card1.setId(UUID.randomUUID());
        card1.setJobTitle("Software Engineer");
        card1.setCompanyName("Google");
        cards.add(card1);

        // 准备 DTO 数据
        BoardDto boardDto = new BoardDto();
        boardDto.setId(boardId);
        boardDto.setName("Test Board");

        ColumnDto colDto1 = new ColumnDto();
        colDto1.setId(col1.getId());
        colDto1.setName("Wish list");

        JobCardDto cardDto1 = new JobCardDto();
        cardDto1.setId(card1.getId());
        cardDto1.setJobTitle("Software Engineer");
        cardDto1.setCompanyName("Google");

        BoardDataDto expectedData = new BoardDataDto();
        expectedData.setBoard(boardDto);
        expectedData.setColumns(Arrays.asList(colDto1));
        expectedData.setCards(Arrays.asList(cardDto1));

        // Mock Repository
        when(boardRepository.findByIdAndUserId(boardId, userId))
                .thenReturn(Optional.of(board));
        when(columnRepository.findByBoardIdOrderBySortOrderAsc(boardId))
                .thenReturn(columns);
        when(jobCardRepository.findByBoardIdAndDeletedAtIsNull(boardId))
                .thenReturn(cards);

        // Mock DtoConverter
        when(dtoConverter.toBoardDataDto(board, columns, cards)).thenReturn(expectedData);

        BoardDataDto result = boardService.loadBoard(userId, request);

        assertNotNull(result);
        assertEquals(boardId, result.getBoard().getId());
        assertEquals(1, result.getColumns().size());
        assertEquals(1, result.getCards().size());

        verify(boardRepository, times(1)).findByIdAndUserId(boardId, userId);
        verify(columnRepository, times(1)).findByBoardIdOrderBySortOrderAsc(boardId);
        verify(jobCardRepository, times(1)).findByBoardIdAndDeletedAtIsNull(boardId);
    }

    @Test
    @DisplayName("测试加载看板 - 未指定 boardId，加载用户第一个看板")
    void testLoadBoard_WithoutBoardId_LoadsFirstBoard() {
        String userId = "test-user-123";
        LoadBoardRequest request = new LoadBoardRequest();

        UUID boardId = UUID.randomUUID();

        Board board = new Board();
        board.setId(boardId);
        board.setUserId(userId);
        board.setName("My First Board");

        BoardDto boardDto = new BoardDto();
        boardDto.setId(boardId);
        boardDto.setName("My First Board");

        BoardDataDto expectedData = new BoardDataDto();
        expectedData.setBoard(boardDto);
        expectedData.setColumns(Collections.emptyList());
        expectedData.setCards(Collections.emptyList());

        when(boardRepository.findFirstByUserIdOrderByCreatedAtAsc(userId))
                .thenReturn(Optional.of(board));
        when(columnRepository.findByBoardIdOrderBySortOrderAsc(boardId))
                .thenReturn(Collections.emptyList());
        when(jobCardRepository.findByBoardIdAndDeletedAtIsNull(boardId))
                .thenReturn(Collections.emptyList());
        when(dtoConverter.toBoardDataDto(board, Collections.emptyList(), Collections.emptyList()))
                .thenReturn(expectedData);

        BoardDataDto result = boardService.loadBoard(userId, request);

        assertNotNull(result);
        assertEquals(boardId, result.getBoard().getId());

        verify(boardRepository, times(1)).findFirstByUserIdOrderByCreatedAtAsc(userId);
    }

    @Test
    @DisplayName("测试加载看板 - 看板不存在时抛出异常")
    void testLoadBoard_BoardNotFound_ThrowsException() {
        UUID nonExistentBoardId = UUID.randomUUID();
        String userId = "test-user-123";
        LoadBoardRequest request = new LoadBoardRequest();
        request.setBoardId(nonExistentBoardId);

        when(boardRepository.findByIdAndUserId(nonExistentBoardId, userId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            boardService.loadBoard(userId, request);
        });

        assertEquals("看板不存在或不属于该用户", exception.getMessage());
    }

    @Test
    @DisplayName("测试加载看板 - 用户没有看板时抛出异常")
    void testLoadBoard_UserHasNoBoards_ThrowsException() {
        String userId = "test-user-123";
        LoadBoardRequest request = new LoadBoardRequest();

        when(boardRepository.findFirstByUserIdOrderByCreatedAtAsc(userId))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            boardService.loadBoard(userId, request);
        });

        assertEquals("没有找到该用户的看板", exception.getMessage());
    }
}
