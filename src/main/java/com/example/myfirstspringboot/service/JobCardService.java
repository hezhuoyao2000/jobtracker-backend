package com.example.myfirstspringboot.service;

import com.example.myfirstspringboot.dto.request.CreateCardRequest;
import com.example.myfirstspringboot.dto.request.DeleteCardRequest;
import com.example.myfirstspringboot.dto.request.MoveCardRequest;
import com.example.myfirstspringboot.dto.request.UpdateCardRequest;
import com.example.myfirstspringboot.dto.response.JobCardDto;

/**
 * 卡片服务接口
 */
public interface JobCardService {

    /**
     * 创建卡片
     * @param userId 当前用户 ID
     * @param request 创建卡片请求参数
     * @return 创建后的卡片信息
     */
    JobCardDto createCard(String userId, CreateCardRequest request);

    /**
     * 更新卡片
     * @param userId 当前用户 ID
     * @param request 更新卡片请求参数
     * @return 更新后的卡片信息
     */
    JobCardDto updateCard(String userId, UpdateCardRequest request);

    /**
     * 移动卡片（变更状态列）
     * @param userId 当前用户 ID
     * @param request 移动卡片请求参数
     * @return 移动后的卡片信息
     */
    JobCardDto moveCard(String userId, MoveCardRequest request);

    /**
     * 软删除卡片
     * @param userId 当前用户 ID
     * @param request 删除卡片请求参数
     */
    void deleteCard(String userId, DeleteCardRequest request);
}
