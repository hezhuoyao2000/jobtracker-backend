package com.example.myfirstspringboot.service.impl;

import com.example.myfirstspringboot.Entity.KanbanColumn;
import com.example.myfirstspringboot.dto.request.UpdateColumnRequest;
import com.example.myfirstspringboot.dto.response.ColumnDto;
import com.example.myfirstspringboot.exception.BusinessException;
import com.example.myfirstspringboot.exception.ResourceNotFoundException;
import com.example.myfirstspringboot.exception.UnauthorizedException;
import com.example.myfirstspringboot.mapper.BoardMapper;
import com.example.myfirstspringboot.mapper.KanbanColumnMapper;
import com.example.myfirstspringboot.util.DtoConverter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * ColumnServiceImpl 单元测试类（MyBatis-Plus 版本）
 */
@ExtendWith(MockitoExtension.class)
class ColumnServiceImplTest {

    // ========== Mock 对象 ==========
    @Mock
    private KanbanColumnMapper columnMapper;

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private DtoConverter dtoConverter;

    @Mock
    private ObjectMapper objectMapper;

    // ========== 被测试对象 ==========
    @InjectMocks
    private ColumnServiceImpl columnService;

    // ========== 测试数据 ==========
    private UUID testColumnId;
    private UUID testBoardId;
    private String testUserId;
    private KanbanColumn testColumn;
    private ColumnDto testColumnDto;

    @BeforeEach
    void setUp() {
        testColumnId = UUID.randomUUID();
        testBoardId = UUID.randomUUID();
        testUserId = "test-user-123";

        testColumn = new KanbanColumn();
        testColumn.setId(testColumnId);
        testColumn.setBoardId(testBoardId);
        testColumn.setName("Applied");
        testColumn.setSortOrder(1);
        testColumn.setIsDefault(true);
        testColumn.setCustomAttributes(null);

        testColumnDto = new ColumnDto();
        testColumnDto.setId(testColumnId);
        testColumnDto.setBoardId(testBoardId);
        testColumnDto.setName("Applied");
        testColumnDto.setSortOrder(1);
        testColumnDto.setIsDefault(true);
    }

    // ============ 正常流程测试 ============ //

    @Test
    @DisplayName("测试更新列 - 只更新名称")
    void testUpdateColumn_UpdateNameOnly() {
        // 准备请求
        UpdateColumnRequest request = new UpdateColumnRequest();
        request.setColumnId(testColumnId);
        request.setName("New Name");

        // Mock
        when(columnMapper.selectById(testColumnId)).thenReturn(testColumn);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(dtoConverter.toColumnDto(testColumn)).thenReturn(testColumnDto);

        // 执行
        ColumnDto result = columnService.updateColumn(testUserId, request);

        // 验证
        assertNotNull(result);
        verify(columnMapper, times(1)).selectById(testColumnId);
        verify(boardMapper, times(1)).existsByIdAndUserId(testBoardId, testUserId);

        // 验证只更新了名称
        ArgumentCaptor<KanbanColumn> columnCaptor = ArgumentCaptor.forClass(KanbanColumn.class);
        verify(columnMapper, times(1)).updateById(columnCaptor.capture());
        KanbanColumn savedColumn = columnCaptor.getValue();
        assertEquals("New Name", savedColumn.getName());
        assertEquals(1, savedColumn.getSortOrder()); // 未改变
    }

    @Test
    @DisplayName("测试更新列 - 只更新排序顺序")
    void testUpdateColumn_UpdateSortOrderOnly() {
        UpdateColumnRequest request = new UpdateColumnRequest();
        request.setColumnId(testColumnId);
        request.setSortOrder(5);

        when(columnMapper.selectById(testColumnId)).thenReturn(testColumn);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(dtoConverter.toColumnDto(testColumn)).thenReturn(testColumnDto);

        ColumnDto result = columnService.updateColumn(testUserId, request);

        assertNotNull(result);

        ArgumentCaptor<KanbanColumn> columnCaptor = ArgumentCaptor.forClass(KanbanColumn.class);
        verify(columnMapper, times(1)).updateById(columnCaptor.capture());
        KanbanColumn savedColumn = columnCaptor.getValue();
        assertEquals("Applied", savedColumn.getName()); // 未改变
        assertEquals(5, savedColumn.getSortOrder());
    }

    @Test
    @DisplayName("测试更新列 - 只更新自定义属性")
    void testUpdateColumn_UpdateCustomAttributesOnly() throws Exception {
        UpdateColumnRequest request = new UpdateColumnRequest();
        request.setColumnId(testColumnId);
        Map<String, Object> customAttrs = new HashMap<>();
        customAttrs.put("color", "#ff0000");
        customAttrs.put("wipLimit", 5);
        request.setCustomAttributes(customAttrs);

        String jsonAttrs = "{\"color\":\"#ff0000\",\"wipLimit\":5}";

        when(columnMapper.selectById(testColumnId)).thenReturn(testColumn);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(objectMapper.writeValueAsString(customAttrs)).thenReturn(jsonAttrs);
        when(dtoConverter.toColumnDto(testColumn)).thenReturn(testColumnDto);

        ColumnDto result = columnService.updateColumn(testUserId, request);

        assertNotNull(result);

        ArgumentCaptor<KanbanColumn> columnCaptor = ArgumentCaptor.forClass(KanbanColumn.class);
        verify(columnMapper, times(1)).updateById(columnCaptor.capture());
        KanbanColumn savedColumn = columnCaptor.getValue();
        assertEquals(jsonAttrs, savedColumn.getCustomAttributes());
        verify(objectMapper, times(1)).writeValueAsString(customAttrs);
    }

    @Test
    @DisplayName("测试更新列 - 更新多个字段")
    void testUpdateColumn_UpdateMultipleFields() throws Exception {
        UpdateColumnRequest request = new UpdateColumnRequest();
        request.setColumnId(testColumnId);
        request.setName("Interviewing Updated");
        request.setSortOrder(3);
        Map<String, Object> customAttrs = new HashMap<>();
        customAttrs.put("icon", "star");
        request.setCustomAttributes(customAttrs);

        String jsonAttrs = "{\"icon\":\"star\"}";

        when(columnMapper.selectById(testColumnId)).thenReturn(testColumn);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(objectMapper.writeValueAsString(customAttrs)).thenReturn(jsonAttrs);
        when(dtoConverter.toColumnDto(testColumn)).thenReturn(testColumnDto);

        ColumnDto result = columnService.updateColumn(testUserId, request);

        assertNotNull(result);

        ArgumentCaptor<KanbanColumn> columnCaptor = ArgumentCaptor.forClass(KanbanColumn.class);
        verify(columnMapper, times(1)).updateById(columnCaptor.capture());
        KanbanColumn savedColumn = columnCaptor.getValue();
        assertEquals("Interviewing Updated", savedColumn.getName());
        assertEquals(3, savedColumn.getSortOrder());
        assertEquals(jsonAttrs, savedColumn.getCustomAttributes());
    }

    @Test
    @DisplayName("测试更新列 - 名称为空时不更新")
    void testUpdateColumn_EmptyName_DoesNotUpdate() {
        UpdateColumnRequest request = new UpdateColumnRequest();
        request.setColumnId(testColumnId);
        request.setName(""); // 空字符串

        when(columnMapper.selectById(testColumnId)).thenReturn(testColumn);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(dtoConverter.toColumnDto(testColumn)).thenReturn(testColumnDto);

        ColumnDto result = columnService.updateColumn(testUserId, request);

        assertNotNull(result);

        ArgumentCaptor<KanbanColumn> columnCaptor = ArgumentCaptor.forClass(KanbanColumn.class);
        verify(columnMapper, times(1)).updateById(columnCaptor.capture());
        KanbanColumn savedColumn = columnCaptor.getValue();
        assertEquals("Applied", savedColumn.getName()); // 保持原值
    }

    @Test
    @DisplayName("测试更新列 - 名称为空白字符时不更新")
    void testUpdateColumn_BlankName_DoesNotUpdate() {
        UpdateColumnRequest request = new UpdateColumnRequest();
        request.setColumnId(testColumnId);
        request.setName("   "); // 空白字符

        when(columnMapper.selectById(testColumnId)).thenReturn(testColumn);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(dtoConverter.toColumnDto(testColumn)).thenReturn(testColumnDto);

        ColumnDto result = columnService.updateColumn(testUserId, request);

        assertNotNull(result);

        ArgumentCaptor<KanbanColumn> columnCaptor = ArgumentCaptor.forClass(KanbanColumn.class);
        verify(columnMapper, times(1)).updateById(columnCaptor.capture());
        KanbanColumn savedColumn = columnCaptor.getValue();
        assertEquals("Applied", savedColumn.getName()); // 保持原值
    }

    // ============ 异常流程测试 ============ //

    @Test
    @DisplayName("测试更新列 - 列不存在时抛出异常")
    void testUpdateColumn_ColumnNotFound_ThrowsException() {
        UUID nonExistentColumnId = UUID.randomUUID();
        UpdateColumnRequest request = new UpdateColumnRequest();
        request.setColumnId(nonExistentColumnId);
        request.setName("New Name");

        when(columnMapper.selectById(nonExistentColumnId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            columnService.updateColumn(testUserId, request);
        });

        assertEquals("列 不存在: id = '" + nonExistentColumnId + "'", exception.getMessage());
        verify(columnMapper, times(1)).selectById(nonExistentColumnId);
        verify(boardMapper, never()).existsByIdAndUserId(any(UUID.class), any(String.class));
        verify(columnMapper, never()).updateById(any(KanbanColumn.class));
    }

    @Test
    @DisplayName("测试更新列 - 看板不属于用户时抛出异常")
    void testUpdateColumn_BoardNotOwned_ThrowsException() {
        UpdateColumnRequest request = new UpdateColumnRequest();
        request.setColumnId(testColumnId);
        request.setName("New Name");

        when(columnMapper.selectById(testColumnId)).thenReturn(testColumn);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(false);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            columnService.updateColumn(testUserId, request);
        });

        assertEquals("无权更新该列", exception.getMessage());
        verify(columnMapper, times(1)).selectById(testColumnId);
        verify(boardMapper, times(1)).existsByIdAndUserId(testBoardId, testUserId);
        verify(columnMapper, never()).updateById(any(KanbanColumn.class));
    }
}
