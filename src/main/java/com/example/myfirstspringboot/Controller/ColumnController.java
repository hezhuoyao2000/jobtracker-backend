package com.example.myfirstspringboot.Controller;

import com.example.myfirstspringboot.dto.request.UpdateColumnRequest;
import com.example.myfirstspringboot.dto.response.ColumnDto;
import com.example.myfirstspringboot.exception.ApiResponse;
import com.example.myfirstspringboot.service.ColumnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Column 控制器
 * <p>
 * 处理看板列相关的 HTTP 请求，包括更新列信息等接口
 * </p>
 *
 * @author yourname
 * @since 2026-03-08
 */
@RestController
@RequestMapping("/board/column")
@RequiredArgsConstructor
@Tag(name = "列管理", description = "看板列相关接口：更新列信息等")
public class ColumnController {

    private final ColumnService columnService;

    /**
     * 更新列信息
     * <p>
     * 接口说明：
     * - 更新指定列的名称、排序顺序或自定义属性
     * - 只更新传入的非空字段，未传入的字段保持原值不变
     * - 会自动校验列所属的看板是否属于当前用户
     * </p>
     *
     * @param request 请求参数
     *                - columnId: 必填，列 ID（UUID 格式）
     *                - name: 可选，列名称
     *                - sortOrder: 可选，排序顺序
     *                - customAttributes: 可选，自定义属性（JSON 对象）
     * @return 更新后的列信息
     *
     * @apiNote 当前 userId 写死为 "user-1"，后续从 JWT Token 中提取
     */
    @Operation(
            summary = "更新列信息",
            description = "更新指定列的信息，包括名称、排序顺序或自定义属性。" +
                    "只更新传入的非空字段，未传入的字段保持原值不变。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(schema = @Schema(implementation = ColumnDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "列不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限修改该列")
    })
    @PostMapping("/update")
    public ApiResponse<ColumnDto> updateColumn(
            @Parameter(description = "更新列请求参数", required = true)
            @RequestBody UpdateColumnRequest request) {
        // TODO: 后续从 JWT Header 中提取 userId
        String userId = "user-1";

        ColumnDto columnDto = columnService.updateColumn(userId, request);
        return ApiResponse.success(columnDto);
    }
}
