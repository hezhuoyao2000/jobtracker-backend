package com.example.myfirstspringboot.service;

import com.example.myfirstspringboot.dto.request.UpdateColumnRequest;
import com.example.myfirstspringboot.dto.response.ColumnDto;

/**
 * 列服务接口
 */
public interface ColumnService {

    /**
     * 更新列信息
     * @param userId 当前用户 ID
     * @param request 更新列请求参数
     * @return 更新后的列信息
     */
    ColumnDto updateColumn(String userId, UpdateColumnRequest request);
}
