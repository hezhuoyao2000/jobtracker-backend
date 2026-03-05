package com.example.myfirstspringboot.service.impl;

import com.example.myfirstspringboot.Entity.Board;
import com.example.myfirstspringboot.Entity.KanbanColumn;
import com.example.myfirstspringboot.dto.request.CreateBoardRequest;
import com.example.myfirstspringboot.dto.request.LoadBoardRequest;
import com.example.myfirstspringboot.dto.response.BoardDataDto;
import com.example.myfirstspringboot.dto.response.BoardDto;
import com.example.myfirstspringboot.dto.response.ColumnDto;
import com.example.myfirstspringboot.dto.response.JobCardDto;
import com.example.myfirstspringboot.mapper.BoardMapper;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * BoardServiceImpl 单元测试类
 *
 * 测试说明：
 * - 使用 Mockito 框架进行单元测试
 * - 不依赖真实数据库，使用 Mock 对象模拟 Repository 和 Mapper
 * - createBoard 使用 JPA 方案（需要 Mock BoardRepository、DtoConverter）
 * - loadBoard 使用 MyBatis 方案（需要 Mock BoardMapper）
 */
@ExtendWith(MockitoExtension.class)
class BoardServiceImplTest {

    // ========== Mock 对象 ==========
    // createBoard 方法使用 JPA 方案，需要以下 Mock：
    @Mock
    private BoardRepository boardRepository;

    @Mock
    private KanbanColumnRepository columnRepository;

    @Mock
    private JobCardRepository jobCardRepository;

    @Mock
    private DtoConverter dtoConverter;

    // loadBoard 方法使用 MyBatis 方案，需要以下 Mock：
    @Mock
    private BoardMapper boardMapper;

    // ========== 被测试对象 ==========

    @InjectMocks  // 将上面的 Mock 对象注入到 BoardServiceImpl 中
    private BoardServiceImpl boardService;

    // ========== 测试数据 ==========

    private UUID testBoardId;
    private String testUserId;
    private CreateBoardRequest createBoardRequest;
    private Board testBoard;
    private BoardDto testBoardDto;

    // ========== 初始化方法 ==========

    @BeforeEach  // 每个测试方法执行前都会运行此方法
    void setUp() {
        // 初始化测试数据
        testBoardId = UUID.randomUUID();
        testUserId = "test-user-123";

        // 创建请求对象
        createBoardRequest = new CreateBoardRequest();
        createBoardRequest.setName("Test Board");

        // 创建测试用的 Board 实体
        testBoard = new Board();
        testBoard.setId(testBoardId);
        testBoard.setUserId(testUserId);
        testBoard.setName("Test Board");

        // 创建测试用的 BoardDto
        testBoardDto = new BoardDto();
        testBoardDto.setId(testBoardId);
        testBoardDto.setUserId(testUserId);
        testBoardDto.setName("Test Board");
    }

    // ========== 测试方法 ==========


    // ============ 创建看板测试 ============ //
    //
    @Test
    @DisplayName("测试创建看板 - 指定名称")
    void testCreateBoard_WithName() {
        // --- Arrange（准备阶段）---
        // 模拟 boardRepository.save() 方法，当传入任何 Board 对象时，返回我们准备好的 testBoard
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);

        // 模拟 dtoConverter.toBoardDto() 方法，当传入 testBoard 时，返回 testBoardDto
        when(dtoConverter.toBoardDto(testBoard)).thenReturn(testBoardDto);

        // --- Act（执行阶段）---
        // 调用被测试的方法
        BoardDto result = boardService.createBoard(testUserId, createBoardRequest);

        // --- Assert（断言阶段）---
        // 验证结果是否正确
        assertNotNull(result, "返回的 BoardDto 不应为 null");
        assertEquals(testBoardId, result.getId(), "看板 ID 应该匹配");
        assertEquals(testUserId, result.getUserId(), "用户 ID 应该匹配");
        assertEquals("Test Board", result.getName(), "看板名称应该匹配");

        // 验证 boardRepository.save() 被调用了一次
        verify(boardRepository, times(1)).save(any(Board.class));

        // 验证 dtoConverter.toBoardDto() 被调用了一次
        verify(dtoConverter, times(1)).toBoardDto(testBoard);

        // 验证 columnRepository.save() 被调用了 5 次（5 个默认列）
        verify(columnRepository, times(5)).save(any(KanbanColumn.class));
    }

    @Test
    @DisplayName("测试创建看板 - 名称为空时使用默认名称")
    void testCreateBoard_WithEmptyName_UsesDefaultName() {
        // --- Arrange ---
        // 设置名称为空
        createBoardRequest.setName(null);

        // 模拟 save 方法返回 testBoard
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        when(dtoConverter.toBoardDto(any(Board.class))).thenReturn(testBoardDto);

        // --- Act ---
        BoardDto result = boardService.createBoard(testUserId, createBoardRequest);

        // --- Assert ---
        assertNotNull(result, "返回的 BoardDto 不应为 null");

        // 捕获传递给 save() 方法的 Board 参数，验证其名称是否为默认值
        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepository, times(1)).save(boardCaptor.capture());

        Board savedBoard = boardCaptor.getValue();
        assertEquals("My Job Tracker", savedBoard.getName(),
            "当名称为空时，应该使用默认名称 'My Job Tracker'");
    }

    @Test
    @DisplayName("测试创建看板 - 名称为纯空格时使用默认名称")
    void testCreateBoard_WithBlankName_UsesDefaultName() {
        // --- Arrange ---
        // 设置名称为纯空格
        createBoardRequest.setName("   ");

        // 模拟 save 方法返回 testBoard
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        when(dtoConverter.toBoardDto(any(Board.class))).thenReturn(testBoardDto);

        // --- Act ---
        BoardDto result = boardService.createBoard(testUserId, createBoardRequest);

        // --- Assert ---
        assertNotNull(result, "返回的 BoardDto 不应为 null");

        // 捕获传递给 save() 方法的 Board 参数，验证其名称是否为默认值
        ArgumentCaptor<Board> boardCaptor = ArgumentCaptor.forClass(Board.class);
        verify(boardRepository, times(1)).save(boardCaptor.capture());

        Board savedBoard = boardCaptor.getValue();
        assertEquals("My Job Tracker", savedBoard.getName(),
            "当名称为纯空格时，应该使用默认名称 'My Job Tracker'");
    }

    @Test
    @DisplayName("测试创建看板 - 验证默认列的创建")
    void testCreateBoard_VerifyDefaultColumns() {
        // --- Arrange ---
        when(boardRepository.save(any(Board.class))).thenReturn(testBoard);
        when(dtoConverter.toBoardDto(any(Board.class))).thenReturn(testBoardDto);

        // --- Act ---
        boardService.createBoard(testUserId, createBoardRequest);

        // --- Assert ---
        // 捕获所有传递给 columnRepository.save() 的 KanbanColumn 参数
        ArgumentCaptor<KanbanColumn> columnCaptor = ArgumentCaptor.forClass(KanbanColumn.class);
        verify(columnRepository, times(5)).save(columnCaptor.capture());

        // 获取所有保存的列
        List<KanbanColumn> savedColumns = columnCaptor.getAllValues();

        // 验证列的数量
        assertEquals(5, savedColumns.size(), "应该创建 5 个默认列");

        // 验证默认的列名
        List<String> expectedColumnNames = Arrays.asList(
            "Wish list", "Applied", "Interviewing", "Offered", "Rejected"
        );

        for (int i = 0; i < savedColumns.size(); i++) {
            KanbanColumn column = savedColumns.get(i);
            assertEquals(expectedColumnNames.get(i), column.getName(),
                "第 " + (i + 1) + " 个列的名称应该匹配");
            assertEquals(testBoardId, column.getBoardId(),
                "列的 boardId 应该与新创建的看板 ID 匹配");
            assertEquals(i, column.getSortOrder(),
                "第 " + (i + 1) + " 个列的 sortOrder 应该是 " + i);
            assertTrue(column.getIsDefault(),
                "第 " + (i + 1) + " 个列的 isDefault 应该为 true");
        }
    }



    // ============ 加载看板测试 ============ //
    // 注意：当前代码使用 MyBatis 联表查询方案，测试需要 Mock BoardMapper

    @Test
    @DisplayName("测试加载看板 - 指定 boardId 成功")
    void testLoadBoard_WithBoardId_Success() {
        // --- Arrange（准备阶段）---

        // 1. 准备测试数据
        UUID boardId = UUID.randomUUID();
        String userId = "test-user-123";

        // 2. 创建请求对象
        LoadBoardRequest request = new LoadBoardRequest();
        request.setBoardId(boardId);

        // 3. 创建期望返回的 DTO（MyBatis 直接返回 BoardDataDto）
        BoardDto expectedBoardDto = new BoardDto();
        expectedBoardDto.setId(boardId);
        expectedBoardDto.setUserId(userId);
        expectedBoardDto.setName("Test Board");

        ColumnDto expectedColumnDto1 = new ColumnDto();
        expectedColumnDto1.setId(UUID.randomUUID());
        expectedColumnDto1.setName("Wish list");

        ColumnDto expectedColumnDto2 = new ColumnDto();
        expectedColumnDto2.setId(UUID.randomUUID());
        expectedColumnDto2.setName("Applied");

        List<ColumnDto> expectedColumnDtos = Arrays.asList(expectedColumnDto1, expectedColumnDto2);

        JobCardDto expectedCardDto = new JobCardDto();
        expectedCardDto.setId(UUID.randomUUID());
        expectedCardDto.setJobTitle("Software Engineer");
        expectedCardDto.setCompanyName("Google");

        List<JobCardDto> expectedCardDtos = Arrays.asList(expectedCardDto);

        BoardDataDto expectedBoardDataDto = new BoardDataDto();
        expectedBoardDataDto.setBoard(expectedBoardDto);
        expectedBoardDataDto.setColumns(expectedColumnDtos);
        expectedBoardDataDto.setCards(expectedCardDtos);

        // 4. 定义 Mock 行为 - MyBatis 方案直接返回 BoardDataDto
        when(boardMapper.findBoardDataByBoardId(boardId, userId))
                .thenReturn(expectedBoardDataDto);

        // --- Act（执行阶段）---

        BoardDataDto result = boardService.loadBoard(userId, request);

        // --- Assert（断言阶段）---

        // 验证结果不为空
        assertNotNull(result, "返回的 BoardDataDto 不应为 null");

        // 验证 board 数据
        assertNotNull(result.getBoard(), "board 不应为 null");
        assertEquals(boardId, result.getBoard().getId(), "看板 ID 应该匹配");
        assertEquals(userId, result.getBoard().getUserId(), "用户 ID 应该匹配");
        assertEquals("Test Board", result.getBoard().getName(), "看板名称应该匹配");

        // 验证 columns 数据
        assertNotNull(result.getColumns(), "columns 不应为 null");
        assertEquals(2, result.getColumns().size(), "应该返回 2 个列");

        // 验证 cards 数据
        assertNotNull(result.getCards(), "cards 不应为 null");
        assertEquals(1, result.getCards().size(), "应该返回 1 个卡片");

        // 验证 MyBatis Mapper 方法被调用
        verify(boardMapper, times(1)).findBoardDataByBoardId(boardId, userId);

        // 验证 JPA Repository 未被调用（因为使用的是 MyBatis 方案）
        verify(boardRepository, never()).findByIdAndUserId(any(), any());
        verify(columnRepository, never()).findByBoardIdOrderBySortOrderAsc(any());
        verify(jobCardRepository, never()).findByBoardIdAndDeletedAtIsNull(any());
    }

    @Test
    @DisplayName("测试加载看板 - 未指定 boardId，加载用户第一个看板")
    void testLoadBoard_WithoutBoardId_LoadsFirstBoard() {
        // --- Arrange ---
        String userId = "test-user-123";
        LoadBoardRequest request = new LoadBoardRequest();
        // boardId 为 null

        UUID boardId = UUID.randomUUID();

        // 创建期望返回的 DTO
        BoardDto expectedBoardDto = new BoardDto();
        expectedBoardDto.setId(boardId);
        expectedBoardDto.setUserId(userId);
        expectedBoardDto.setName("My First Board");

        BoardDataDto expectedBoardDataDto = new BoardDataDto();
        expectedBoardDataDto.setBoard(expectedBoardDto);
        expectedBoardDataDto.setColumns(Collections.emptyList());
        expectedBoardDataDto.setCards(Collections.emptyList());

        // 定义 Mock 行为 - MyBatis 方案
        when(boardMapper.findBoardDataByUserId(userId))
                .thenReturn(expectedBoardDataDto);

        // --- Act ---
        BoardDataDto result = boardService.loadBoard(userId, request);

        // --- Assert ---
        assertNotNull(result, "返回的 BoardDataDto 不应为 null");
        assertEquals(boardId, result.getBoard().getId(), "应该返回用户的第一个看板");
        assertEquals("My First Board", result.getBoard().getName(), "看板名称应该匹配");

        // 验证调用了 MyBatis Mapper
        verify(boardMapper, times(1)).findBoardDataByUserId(userId);

        // 验证 JPA Repository 未被调用
        verify(boardRepository, never()).findByUserIdOrderByCreatedAtAsc(any());
    }

    @Test
    @DisplayName("测试加载看板 - 看板不存在时抛出异常")
    void testLoadBoard_BoardNotFound_ThrowsException() {
        // --- Arrange ---
        UUID nonExistentBoardId = UUID.randomUUID();
        String userId = "test-user-123";
        LoadBoardRequest request = new LoadBoardRequest();
        request.setBoardId(nonExistentBoardId);

        // 定义 Mock 行为：MyBatis 返回 null 表示看板不存在
        when(boardMapper.findBoardDataByBoardId(nonExistentBoardId, userId))
                .thenReturn(null);

        // --- Act & Assert ---
        // 验证抛出 RuntimeException
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            boardService.loadBoard(userId, request);
        });

        assertEquals("看板不存在", exception.getMessage(), "异常消息应该匹配");
    }

    @Test
    @DisplayName("测试加载看板 - 无权访问时抛出异常")
    void testLoadBoard_PermissionDenied_ThrowsException() {
        // --- Arrange ---
        UUID boardId = UUID.randomUUID();
        String userId = "test-user-123";
        LoadBoardRequest request = new LoadBoardRequest();
        request.setBoardId(boardId);

        // 定义 Mock 行为：MyBatis 返回 null 表示无权访问（看板不属于该用户）
        when(boardMapper.findBoardDataByBoardId(boardId, userId))
                .thenReturn(null);

        // --- Act & Assert ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            boardService.loadBoard(userId, request);
        });

        assertEquals("看板不存在", exception.getMessage(), "异常消息应该匹配");
    }

    @Test
    @DisplayName("测试加载看板 - 用户没有看板时抛出异常")
    void testLoadBoard_UserHasNoBoards_ThrowsException() {
        // --- Arrange ---
        String userId = "test-user-123";
        LoadBoardRequest request = new LoadBoardRequest();
        // boardId 为 null，应该加载用户的第一个看板

        // 定义 Mock 行为：MyBatis 返回空数据
        when(boardMapper.findBoardDataByUserId(userId))
                .thenReturn(null);

        // --- Act & Assert ---
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            boardService.loadBoard(userId, request);
        });

        assertEquals("看板不存在", exception.getMessage(), "异常消息应该匹配");
    }

    @Test
    @DisplayName("测试加载看板 - 验证返回数据的完整性")
    void testLoadBoard_VerifyReturnedData() {
        // --- Arrange ---
        UUID boardId = UUID.randomUUID();
        String userId = "test-user-123";
        LoadBoardRequest request = new LoadBoardRequest();
        request.setBoardId(boardId);

        // 创建期望返回的 DTO
        BoardDto expectedBoardDto = new BoardDto();
        expectedBoardDto.setId(boardId);
        expectedBoardDto.setName("Complete Board");

        // 创建 5 个默认列的 DTO
        List<String> columnNames = Arrays.asList("Wish list", "Applied", "Interviewing", "Offered", "Rejected");
        List<ColumnDto> expectedColumnDtos = new ArrayList<>();
        for (int i = 0; i < columnNames.size(); i++) {
            ColumnDto columnDto = new ColumnDto();
            columnDto.setId(UUID.randomUUID());
            columnDto.setName(columnNames.get(i));
            columnDto.setSortOrder(i);
            expectedColumnDtos.add(columnDto);
        }

        // 创建 3 张卡片的 DTO
        List<JobCardDto> expectedCardDtos = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            JobCardDto cardDto = new JobCardDto();
            cardDto.setId(UUID.randomUUID());
            cardDto.setJobTitle("Job " + i);
            cardDto.setCompanyName("Company " + i);
            expectedCardDtos.add(cardDto);
        }

        BoardDataDto expectedBoardDataDto = new BoardDataDto();
        expectedBoardDataDto.setBoard(expectedBoardDto);
        expectedBoardDataDto.setColumns(expectedColumnDtos);
        expectedBoardDataDto.setCards(expectedCardDtos);

        // 定义 Mock 行为 - MyBatis 方案
        when(boardMapper.findBoardDataByBoardId(boardId, userId))
                .thenReturn(expectedBoardDataDto);

        // --- Act ---
        BoardDataDto result = boardService.loadBoard(userId, request);

        // --- Assert ---
        // 验证调用了 MyBatis Mapper
        verify(boardMapper, times(1)).findBoardDataByBoardId(boardId, userId);

        // 验证 JPA Repository 未被调用
        verify(boardRepository, never()).findByIdAndUserId(any(), any());
        verify(columnRepository, never()).findByBoardIdOrderBySortOrderAsc(any());
        verify(jobCardRepository, never()).findByBoardIdAndDeletedAtIsNull(any());

        // 验证 DtoConverter 未被调用（MyBatis 直接返回 DTO）
        verify(dtoConverter, never()).toBoardDto(any());
        verify(dtoConverter, never()).toColumnDtoList(any());
        verify(dtoConverter, never()).toJobCardDtoList(any());
    }
}
