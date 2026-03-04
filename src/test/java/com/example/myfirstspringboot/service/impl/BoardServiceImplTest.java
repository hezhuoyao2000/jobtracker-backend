package com.example.myfirstspringboot.service.impl;

import com.example.myfirstspringboot.Entity.Board;
import com.example.myfirstspringboot.Entity.KanbanColumn;
import com.example.myfirstspringboot.dto.request.CreateBoardRequest;
import com.example.myfirstspringboot.dto.response.BoardDto;
import com.example.myfirstspringboot.mapper.BoardMapper;
import com.example.myfirstspringboot.repository.BoardRepository;
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
 * - 测试 createBoard 方法的业务逻辑
 */
@ExtendWith(MockitoExtension.class)  // 启用 Mockito 测试支持
class BoardServiceImplTest {

    // ========== Mock 对象 ==========

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private KanbanColumnRepository columnRepository;

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private DtoConverter dtoConverter;

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
}


