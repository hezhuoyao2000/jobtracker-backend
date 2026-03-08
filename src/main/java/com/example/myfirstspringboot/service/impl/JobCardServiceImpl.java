package com.example.myfirstspringboot.service.impl;

import com.example.myfirstspringboot.Entity.JobCard;
import com.example.myfirstspringboot.dto.request.CreateCardRequest;
import com.example.myfirstspringboot.dto.request.DeleteCardRequest;
import com.example.myfirstspringboot.dto.request.MoveCardRequest;
import com.example.myfirstspringboot.dto.request.UpdateCardRequest;
import com.example.myfirstspringboot.dto.response.JobCardDto;
import com.example.myfirstspringboot.repository.BoardRepository;
import com.example.myfirstspringboot.repository.JobCardRepository;
import com.example.myfirstspringboot.repository.KanbanColumnRepository;
import com.example.myfirstspringboot.service.JobCardService;
import com.example.myfirstspringboot.util.DtoConverter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * 卡片服务实现类
 */
@Service
@RequiredArgsConstructor
public class JobCardServiceImpl implements JobCardService {

    private final JobCardRepository jobCardRepository;
    private final BoardRepository boardRepository;
    private final KanbanColumnRepository columnRepository;
    private final DtoConverter dtoConverter;
    private final ObjectMapper objectMapper;

    /**
     * 创建卡片
     * @param userId 当前用户 ID
     * @param request 创建卡片请求参数
     * @return 创建后的卡片信息
     */
    @Override
    @Transactional
    public JobCardDto createCard(String userId, CreateCardRequest request) {
        UUID boardId = request.getBoardId();
        UUID statusId = request.getStatusId();

        // 步骤 1: 校验看板属于当前用户
        boolean boardExists = boardRepository.existsByIdAndUserId(boardId, userId);
        if (!boardExists) {
            throw new RuntimeException("看板不存在或不属于该用户");
        }

        // 步骤 2: 校验 statusId 对应的列属于该看板
        boolean columnExists = columnRepository.existsByIdAndBoardId(statusId, boardId);
        if (!columnExists) {
            throw new RuntimeException("指定的列不存在或不属于该看板");
        }

        // 步骤 3: 构建 JobCard
        JobCard jobCard = new JobCard();
        jobCard.setBoardId(boardId);
        jobCard.setStatusId(statusId);
        jobCard.setJobTitle(request.getJobTitle());
        jobCard.setCompanyName(request.getCompanyName());
        jobCard.setJobLink(request.getJobLink());
        jobCard.setSourcePlatform(request.getSourcePlatform());
        jobCard.setJobLocation(request.getJobLocation());
        jobCard.setDescription(request.getDescription());

        // 处理 tags 数组（序列化为 JSON 字符串）
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            try {
                jobCard.setTags(objectMapper.writeValueAsString(request.getTags()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("标签序列化失败", e);
            }
        }

        jobCard.setComments(request.getComments());

        // 处理 extra 对象（序列化为 JSON 字符串）
        if (request.getExtra() != null) {
            try {
                jobCard.setExtra(objectMapper.writeValueAsString(request.getExtra()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("扩展字段序列化失败", e);
            }
        }

        // 步骤 4: 保存到数据库
        JobCard savedCard = jobCardRepository.save(jobCard);

        // 步骤 5: 转换为 DTO 并返回
        return dtoConverter.toJobCardDto(savedCard);
    }

    /**
     * 更新卡片
     * @param userId 当前用户 ID
     * @param request 更新卡片请求参数
     * @return 更新后的卡片信息
     */
    @Override
    @Transactional
    public JobCardDto updateCard(String userId, UpdateCardRequest request) {
        UUID cardId = request.getCardId();

        // 步骤 1: 查询卡片（未删除的）并校验权限
        JobCard jobCard = jobCardRepository.findByIdAndDeletedAtIsNull(cardId)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));

        UUID boardId = jobCard.getBoardId();
        boolean boardExists = boardRepository.existsByIdAndUserId(boardId, userId);
        if (!boardExists) {
            throw new RuntimeException("看板不存在或不属于该用户");
        }

        // 步骤 2: 如果更新 statusId，校验新列属于该看板
        if (request.getStatusId() != null) {
            boolean columnExists = columnRepository.existsByIdAndBoardId(request.getStatusId(), boardId);
            if (!columnExists) {
                throw new RuntimeException("指定的列不存在或不属于该看板");
            }
            jobCard.setStatusId(request.getStatusId());
        }

        // 步骤 3: 只更新传入的非空字段
        if (request.getJobTitle() != null) {
            jobCard.setJobTitle(request.getJobTitle());
        }
        if (request.getCompanyName() != null) {
            jobCard.setCompanyName(request.getCompanyName());
        }
        if (request.getJobLink() != null) {
            jobCard.setJobLink(request.getJobLink());
        }
        if (request.getSourcePlatform() != null) {
            jobCard.setSourcePlatform(request.getSourcePlatform());
        }
        if (request.getJobLocation() != null) {
            jobCard.setJobLocation(request.getJobLocation());
        }
        if (request.getDescription() != null) {
            jobCard.setDescription(request.getDescription());
        }
        if (request.getComments() != null) {
            jobCard.setComments(request.getComments());
        }

        // 更新 tags
        if (request.getTags() != null) {
            try {
                jobCard.setTags(objectMapper.writeValueAsString(request.getTags()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("标签序列化失败", e);
            }
        }

        // 更新 extra
        if (request.getExtra() != null) {
            try {
                jobCard.setExtra(objectMapper.writeValueAsString(request.getExtra()));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("扩展字段序列化失败", e);
            }
        }

        // 步骤 4: 保存到数据库
        JobCard savedCard = jobCardRepository.save(jobCard);

        // 步骤 5: 转换为 DTO 并返回
        return dtoConverter.toJobCardDto(savedCard);
    }

    /**
     * 移动卡片（变更状态列）
     * @param userId 当前用户 ID
     * @param request 移动卡片请求参数
     * @return 移动后的卡片信息
     */
    @Override
    @Transactional
    public JobCardDto moveCard(String userId, MoveCardRequest request) {
        UUID cardId = request.getCardId();
        UUID targetStatusId = request.getTargetStatusId();

        // 步骤 1: 查询卡片（未删除的）并校验权限
        JobCard jobCard = jobCardRepository.findByIdAndDeletedAtIsNull(cardId)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));

        UUID boardId = jobCard.getBoardId();
        boolean boardExists = boardRepository.existsByIdAndUserId(boardId, userId);
        if (!boardExists) {
            throw new RuntimeException("看板不存在或不属于该用户");
        }

        // 步骤 2: 校验 targetStatusId 对应的列属于该看板
        boolean columnExists = columnRepository.existsByIdAndBoardId(targetStatusId, boardId);
        if (!columnExists) {
            throw new RuntimeException("目标列不存在或不属于该看板");
        }

        // 步骤 3: 更新卡片状态
        jobCard.setStatusId(targetStatusId);

        // 步骤 4: 保存到数据库
        JobCard savedCard = jobCardRepository.save(jobCard);

        // 步骤 5: 转换为 DTO 并返回
        return dtoConverter.toJobCardDto(savedCard);
    }

    /**
     * 软删除卡片
     * @param userId 当前用户 ID
     * @param request 删除卡片请求参数
     */
    @Override
    @Transactional
    public void deleteCard(String userId, DeleteCardRequest request) {
        UUID cardId = request.getCardId();

        // 步骤 1: 查询卡片（未删除的）并校验权限
        JobCard jobCard = jobCardRepository.findByIdAndDeletedAtIsNull(cardId)
                .orElseThrow(() -> new RuntimeException("卡片不存在"));

        UUID boardId = jobCard.getBoardId();
        boolean boardExists = boardRepository.existsByIdAndUserId(boardId, userId);
        if (!boardExists) {
            throw new RuntimeException("看板不存在或不属于该用户");
        }

        // 步骤 2: 设置删除时间（软删除）
        jobCard.setDeletedAt(Instant.now());

        // 步骤 3: 保存到数据库
        jobCardRepository.save(jobCard);
    }
}
