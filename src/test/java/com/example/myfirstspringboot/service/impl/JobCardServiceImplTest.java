package com.example.myfirstspringboot.service.impl;

import com.example.myfirstspringboot.Entity.JobCard;
import com.example.myfirstspringboot.dto.request.CreateCardRequest;
import com.example.myfirstspringboot.dto.request.DeleteCardRequest;
import com.example.myfirstspringboot.dto.request.MoveCardRequest;
import com.example.myfirstspringboot.dto.request.UpdateCardRequest;
import com.example.myfirstspringboot.dto.response.JobCardDto;
import com.example.myfirstspringboot.exception.BusinessException;
import com.example.myfirstspringboot.exception.ResourceNotFoundException;
import com.example.myfirstspringboot.exception.UnauthorizedException;
import com.example.myfirstspringboot.mapper.BoardMapper;
import com.example.myfirstspringboot.mapper.JobCardMapper;
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

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * JobCardServiceImpl 单元测试类（MyBatis-Plus 版本）
 */
@ExtendWith(MockitoExtension.class)
class JobCardServiceImplTest {

    // ========== Mock 对象 ==========
    @Mock
    private JobCardMapper jobCardMapper;

    @Mock
    private BoardMapper boardMapper;

    @Mock
    private KanbanColumnMapper columnMapper;

    @Mock
    private DtoConverter dtoConverter;

    @Mock
    private ObjectMapper objectMapper;

    // ========== 被测试对象 ==========
    @InjectMocks
    private JobCardServiceImpl jobCardService;

    // ========== 测试数据 ==========
    private UUID testCardId;
    private UUID testBoardId;
    private UUID testStatusId;
    private String testUserId;
    private JobCard testJobCard;
    private JobCardDto testJobCardDto;

    @BeforeEach
    void setUp() {
        testCardId = UUID.randomUUID();
        testBoardId = UUID.randomUUID();
        testStatusId = UUID.randomUUID();
        testUserId = "test-user-123";

        testJobCard = new JobCard();
        testJobCard.setId(testCardId);
        testJobCard.setBoardId(testBoardId);
        testJobCard.setStatusId(testStatusId);
        testJobCard.setJobTitle("Software Engineer");
        testJobCard.setCompanyName("Google");
        testJobCard.setJobLink("https://jobs.google.com");
        testJobCard.setSourcePlatform("LinkedIn");
        testJobCard.setExpired(false);
        testJobCard.setJobLocation("Mountain View");
        testJobCard.setDescription("Senior software engineer position");
        testJobCard.setAppliedTime(Instant.now());
        testJobCard.setTags("[\"java\", \"spring\"]");
        testJobCard.setComments("Good opportunity");
        testJobCard.setExtra("{\"salary\": \"150k\"}");
        testJobCard.setCreatedAt(Instant.now());
        testJobCard.setUpdatedAt(Instant.now());
        testJobCard.setDeletedAt(null);

        testJobCardDto = new JobCardDto();
        testJobCardDto.setId(testCardId);
        testJobCardDto.setBoardId(testBoardId);
        testJobCardDto.setStatusId(testStatusId);
        testJobCardDto.setJobTitle("Software Engineer");
        testJobCardDto.setCompanyName("Google");
    }

    // ============ 创建卡片测试 ============ //

    @Test
    @DisplayName("测试创建卡片 - 成功创建")
    void testCreateCard_Success() throws Exception {
        // 准备请求
        CreateCardRequest request = new CreateCardRequest();
        request.setBoardId(testBoardId);
        request.setStatusId(testStatusId);
        request.setJobTitle("Software Engineer");
        request.setCompanyName("Google");
        request.setJobLink("https://jobs.google.com");
        request.setSourcePlatform("LinkedIn");
        request.setJobLocation("Mountain View");
        request.setDescription("Senior position");
        request.setTags(Arrays.asList("java", "spring"));
        request.setComments("Good opportunity");
        request.setExtra(new HashMap<String, Object>() {{ put("salary", "150k"); }});

        // Mock
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(columnMapper.existsByIdAndBoardId(testStatusId, testBoardId)).thenReturn(true);
        when(objectMapper.writeValueAsString(request.getTags())).thenReturn("[\"java\", \"spring\"]");
        when(objectMapper.writeValueAsString(request.getExtra())).thenReturn("{\"salary\": \"150k\"}");
        when(dtoConverter.toJobCardDto(any(JobCard.class))).thenReturn(testJobCardDto);

        // 执行
        JobCardDto result = jobCardService.createCard(testUserId, request);

        // 验证
        assertNotNull(result);
        assertEquals(testCardId, result.getId());
        verify(boardMapper, times(1)).existsByIdAndUserId(testBoardId, testUserId);
        verify(columnMapper, times(1)).existsByIdAndBoardId(testStatusId, testBoardId);

        ArgumentCaptor<JobCard> cardCaptor = ArgumentCaptor.forClass(JobCard.class);
        verify(jobCardMapper, times(1)).insert(cardCaptor.capture());
        JobCard savedCard = cardCaptor.getValue();
        assertEquals("Software Engineer", savedCard.getJobTitle());
        assertEquals("Google", savedCard.getCompanyName());
        assertEquals(testBoardId, savedCard.getBoardId());
        assertEquals(testStatusId, savedCard.getStatusId());
    }

    @Test
    @DisplayName("测试创建卡片 - 看板不存在时抛出异常")
    void testCreateCard_BoardNotFound_ThrowsException() {
        CreateCardRequest request = new CreateCardRequest();
        request.setBoardId(testBoardId);
        request.setStatusId(testStatusId);
        request.setJobTitle("Software Engineer");
        request.setCompanyName("Google");

        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(false);

        UnauthorizedException exception = assertThrows(UnauthorizedException.class, () -> {
            jobCardService.createCard(testUserId, request);
        });

        assertEquals("无权在该看板创建卡片", exception.getMessage());
        verify(columnMapper, never()).existsByIdAndBoardId(any(UUID.class), any(UUID.class));
        verify(jobCardMapper, never()).insert(any(JobCard.class));
    }

    @Test
    @DisplayName("测试创建卡片 - 列不存在时抛出异常")
    void testCreateCard_ColumnNotFound_ThrowsException() {
        CreateCardRequest request = new CreateCardRequest();
        request.setBoardId(testBoardId);
        request.setStatusId(testStatusId);
        request.setJobTitle("Software Engineer");
        request.setCompanyName("Google");

        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(columnMapper.existsByIdAndBoardId(testStatusId, testBoardId)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            jobCardService.createCard(testUserId, request);
        });

        assertEquals("指定的列不存在或不属于该看板", exception.getMessage());
        verify(jobCardMapper, never()).insert(any(JobCard.class));
    }

    // ============ 更新卡片测试 ============ //

    @Test
    @DisplayName("测试更新卡片 - 成功更新")
    void testUpdateCard_Success() throws Exception {
        UpdateCardRequest request = new UpdateCardRequest();
        request.setCardId(testCardId);
        request.setJobTitle("Updated Title");
        request.setCompanyName("Updated Company");

        when(jobCardMapper.selectByIdAndDeletedAtIsNull(testCardId)).thenReturn(testJobCard);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(dtoConverter.toJobCardDto(any(JobCard.class))).thenReturn(testJobCardDto);

        JobCardDto result = jobCardService.updateCard(testUserId, request);

        assertNotNull(result);
        verify(jobCardMapper, times(1)).selectByIdAndDeletedAtIsNull(testCardId);
        verify(boardMapper, times(1)).existsByIdAndUserId(testBoardId, testUserId);

        ArgumentCaptor<JobCard> cardCaptor = ArgumentCaptor.forClass(JobCard.class);
        verify(jobCardMapper, times(1)).updateById(cardCaptor.capture());
        JobCard savedCard = cardCaptor.getValue();
        assertEquals("Updated Title", savedCard.getJobTitle());
        assertEquals("Updated Company", savedCard.getCompanyName());
    }

    @Test
    @DisplayName("测试更新卡片 - 更新 statusId 成功")
    void testUpdateCard_UpdateStatusId_Success() {
        UUID newStatusId = UUID.randomUUID();
        UpdateCardRequest request = new UpdateCardRequest();
        request.setCardId(testCardId);
        request.setStatusId(newStatusId);

        when(jobCardMapper.selectByIdAndDeletedAtIsNull(testCardId)).thenReturn(testJobCard);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(columnMapper.existsByIdAndBoardId(newStatusId, testBoardId)).thenReturn(true);
        when(dtoConverter.toJobCardDto(any(JobCard.class))).thenReturn(testJobCardDto);

        JobCardDto result = jobCardService.updateCard(testUserId, request);

        assertNotNull(result);
        verify(columnMapper, times(1)).existsByIdAndBoardId(newStatusId, testBoardId);

        ArgumentCaptor<JobCard> cardCaptor = ArgumentCaptor.forClass(JobCard.class);
        verify(jobCardMapper, times(1)).updateById(cardCaptor.capture());
        JobCard savedCard = cardCaptor.getValue();
        assertEquals(newStatusId, savedCard.getStatusId());
    }

    @Test
    @DisplayName("测试更新卡片 - 卡片不存在时抛出异常")
    void testUpdateCard_CardNotFound_ThrowsException() {
        UUID nonExistentCardId = UUID.randomUUID();
        UpdateCardRequest request = new UpdateCardRequest();
        request.setCardId(nonExistentCardId);
        request.setJobTitle("Updated Title");

        when(jobCardMapper.selectByIdAndDeletedAtIsNull(nonExistentCardId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            jobCardService.updateCard(testUserId, request);
        });

        assertEquals("卡片 不存在: id = '" + nonExistentCardId + "'", exception.getMessage());
        verify(boardMapper, never()).existsByIdAndUserId(any(UUID.class), any(String.class));
        verify(jobCardMapper, never()).updateById(any(JobCard.class));
    }

    @Test
    @DisplayName("测试更新卡片 - statusId 对应列不存在时抛出异常")
    void testUpdateCard_InvalidStatusId_ThrowsException() {
        UUID invalidStatusId = UUID.randomUUID();
        UpdateCardRequest request = new UpdateCardRequest();
        request.setCardId(testCardId);
        request.setStatusId(invalidStatusId);

        when(jobCardMapper.selectByIdAndDeletedAtIsNull(testCardId)).thenReturn(testJobCard);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(columnMapper.existsByIdAndBoardId(invalidStatusId, testBoardId)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            jobCardService.updateCard(testUserId, request);
        });

        assertEquals("指定的列不存在或不属于该看板", exception.getMessage());
        verify(jobCardMapper, never()).updateById(any(JobCard.class));
    }

    // ============ 移动卡片测试 ============ //

    @Test
    @DisplayName("测试移动卡片 - 成功移动")
    void testMoveCard_Success() {
        UUID newStatusId = UUID.randomUUID();
        MoveCardRequest request = new MoveCardRequest();
        request.setCardId(testCardId);
        request.setTargetStatusId(newStatusId);

        when(jobCardMapper.selectByIdAndDeletedAtIsNull(testCardId)).thenReturn(testJobCard);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(columnMapper.existsByIdAndBoardId(newStatusId, testBoardId)).thenReturn(true);
        when(dtoConverter.toJobCardDto(any(JobCard.class))).thenReturn(testJobCardDto);

        JobCardDto result = jobCardService.moveCard(testUserId, request);

        assertNotNull(result);
        verify(jobCardMapper, times(1)).selectByIdAndDeletedAtIsNull(testCardId);
        verify(boardMapper, times(1)).existsByIdAndUserId(testBoardId, testUserId);
        verify(columnMapper, times(1)).existsByIdAndBoardId(newStatusId, testBoardId);

        ArgumentCaptor<JobCard> cardCaptor = ArgumentCaptor.forClass(JobCard.class);
        verify(jobCardMapper, times(1)).updateById(cardCaptor.capture());
        JobCard savedCard = cardCaptor.getValue();
        assertEquals(newStatusId, savedCard.getStatusId());
    }

    @Test
    @DisplayName("测试移动卡片 - 目标列不存在时抛出异常")
    void testMoveCard_TargetColumnNotFound_ThrowsException() {
        UUID invalidStatusId = UUID.randomUUID();
        MoveCardRequest request = new MoveCardRequest();
        request.setCardId(testCardId);
        request.setTargetStatusId(invalidStatusId);

        when(jobCardMapper.selectByIdAndDeletedAtIsNull(testCardId)).thenReturn(testJobCard);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);
        when(columnMapper.existsByIdAndBoardId(invalidStatusId, testBoardId)).thenReturn(false);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            jobCardService.moveCard(testUserId, request);
        });

        assertEquals("目标列不存在或不属于该看板", exception.getMessage());
        verify(jobCardMapper, never()).updateById(any(JobCard.class));
    }

    // ============ 删除卡片测试 ============ //

    @Test
    @DisplayName("测试删除卡片 - 成功软删除")
    void testDeleteCard_Success() {
        DeleteCardRequest request = new DeleteCardRequest();
        request.setCardId(testCardId);

        when(jobCardMapper.selectByIdAndDeletedAtIsNull(testCardId)).thenReturn(testJobCard);
        when(boardMapper.existsByIdAndUserId(testBoardId, testUserId)).thenReturn(true);

        // 执行（无返回值）
        assertDoesNotThrow(() -> jobCardService.deleteCard(testUserId, request));

        verify(jobCardMapper, times(1)).selectByIdAndDeletedAtIsNull(testCardId);
        verify(boardMapper, times(1)).existsByIdAndUserId(testBoardId, testUserId);

        ArgumentCaptor<JobCard> cardCaptor = ArgumentCaptor.forClass(JobCard.class);
        verify(jobCardMapper, times(1)).updateById(cardCaptor.capture());
        JobCard savedCard = cardCaptor.getValue();
        assertNotNull(savedCard.getDeletedAt());
    }

    @Test
    @DisplayName("测试删除卡片 - 卡片不存在时抛出异常")
    void testDeleteCard_CardNotFound_ThrowsException() {
        UUID nonExistentCardId = UUID.randomUUID();
        DeleteCardRequest request = new DeleteCardRequest();
        request.setCardId(nonExistentCardId);

        when(jobCardMapper.selectByIdAndDeletedAtIsNull(nonExistentCardId)).thenReturn(null);

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> {
            jobCardService.deleteCard(testUserId, request);
        });

        assertEquals("卡片 不存在: id = '" + nonExistentCardId + "'", exception.getMessage());
        verify(jobCardMapper, never()).updateById(any(JobCard.class));
    }
}
