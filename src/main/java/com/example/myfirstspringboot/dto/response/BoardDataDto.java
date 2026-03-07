package com.example.myfirstspringboot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

/**
 * 看板完整数据响应
 */
@Data
@Schema(description = "看板完整数据，包含看板信息、列列表和卡片列表")
public class BoardDataDto {

    @Schema(description = "看板基本信息")
    private BoardDto board;

    @Schema(description = "看板列列表")
    private List<ColumnDto> columns;

    @Schema(description = "看板卡片列表（未删除的）")
    private List<JobCardDto> cards;
}
