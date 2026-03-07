package com.example.myfirstspringboot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.Map;
import java.util.UUID;

/**
 * 看板列信息响应
 */
@Data
@Schema(description = "看板列信息")
public class ColumnDto {

    @Schema(description = "列 ID", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID id;

    @Schema(description = "所属看板 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID boardId;

    @Schema(description = "列名称", example = "Applied")
    private String name;

    @Schema(description = "排序顺序", example = "1")
    private Integer sortOrder;

    @Schema(description = "是否为系统默认列", example = "true")
    private Boolean isDefault;

    @Schema(description = "自定义属性")
    private Map<String, Object> customAttributes;
}
