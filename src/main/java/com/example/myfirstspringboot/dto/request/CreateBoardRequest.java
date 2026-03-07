package com.example.myfirstspringboot.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 创建看板请求
 */
@Data
@Schema(description = "创建看板请求参数")
public class CreateBoardRequest {

    @Schema(description = "看板名称，为空则使用默认名称 'My Job Tracker'", example = "My Job Tracker")
    private String name;
}
