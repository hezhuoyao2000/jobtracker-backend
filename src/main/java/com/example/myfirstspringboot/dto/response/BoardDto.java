package com.example.myfirstspringboot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.UUID;

/**
 * 看板信息响应
 */
@Data
@Schema(description = "看板信息")
public class BoardDto {

    @Schema(description = "看板 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @Schema(description = "用户 ID", example = "user-1")
    private String userId;

    @Schema(description = "看板名称", example = "My Job Tracker")
    private String name;

    @Schema(description = "创建时间", example = "2026-03-07T10:30:00Z")
    private String createdAt;

    @Schema(description = "更新时间", example = "2026-03-07T10:30:00Z")
    private String updatedAt;
}
