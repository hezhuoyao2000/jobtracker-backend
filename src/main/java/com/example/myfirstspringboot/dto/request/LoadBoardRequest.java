package com.example.myfirstspringboot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.UUID;

/**
 * 加载看板请求
 */
@Data
@Schema(description = "加载看板请求参数")
public class LoadBoardRequest {

    @Schema(description = "看板 ID（UUID 格式），不传则返回用户的第一个看板", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID boardId;
}
