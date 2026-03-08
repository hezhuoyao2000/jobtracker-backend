package com.example.myfirstspringboot.Controller;

import com.example.myfirstspringboot.dto.request.CreateCardRequest;
import com.example.myfirstspringboot.dto.request.DeleteCardRequest;
import com.example.myfirstspringboot.dto.request.MoveCardRequest;
import com.example.myfirstspringboot.dto.request.UpdateCardRequest;
import com.example.myfirstspringboot.dto.response.JobCardDto;
import com.example.myfirstspringboot.exception.ApiResponse;
import com.example.myfirstspringboot.exception.UnauthorizedException;
import com.example.myfirstspringboot.service.JobCardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * JobCard 控制器
 *
 * <p>
 * 处理职位卡片相关的 HTTP 请求，包括创建、更新、移动、删除卡片等接口
 * </p>
 *
 * @author yourname
 * @since 2026-03-08
 */
@RestController
@RequestMapping("/board/card")
@RequiredArgsConstructor
@Tag(name = "卡片管理", description = "职位卡片相关接口：创建、更新、移动、删除卡片等")
public class JobCardController {

    private final JobCardService jobCardService;

    /**
     * 从请求中获取当前用户 ID
     */
    private String getCurrentUserId(HttpServletRequest request) {
        String userId = (String) request.getAttribute("userId");
        if (userId == null) {
            throw new UnauthorizedException("未登录或登录已过期");
        }
        return userId;
    }

    /**
     * 创建卡片
     * <p>
     * 接口说明：
     * - 在指定看板的指定列中创建新卡片
     * - 会自动校验看板属于当前用户，列属于该看板
     * - tags 和 extra 字段为可选
     * </p>
     *
     * @param request     请求参数
     *                    - boardId: 必填，看板 ID
     *                    - statusId: 必填，列 ID（卡片初始状态）
     *                    - jobTitle: 必填，职位名称
     *                    - companyName: 必填，公司名称
     *                    - 其他字段可选
     * @param httpRequest HTTP 请求（用于获取当前用户 ID）
     * @return 创建后的卡片信息
     */
    @Operation(
            summary = "创建卡片",
            description = "在指定看板的指定列中创建新职位卡片。" +
                    "必填字段：boardId、statusId、jobTitle、companyName"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(schema = @Schema(implementation = JobCardDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "看板或列不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
    @PostMapping("/create")
    public ApiResponse<JobCardDto> createCard(
            @Parameter(description = "创建卡片请求参数", required = true)
            @RequestBody CreateCardRequest request,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        JobCardDto cardDto = jobCardService.createCard(userId, request);
        return ApiResponse.success(cardDto);
    }

    /**
     * 更新卡片
     * <p>
     * 接口说明：
     * - 更新指定卡片的信息
     * - 只更新传入的非空字段，未传入的字段保持原值不变
     * - 支持更新 statusId 来移动卡片到其他列
     * </p>
     *
     * @param request     请求参数
     *                    - cardId: 必填，卡片 ID
     *                    - 其他字段可选
     * @param httpRequest HTTP 请求（用于获取当前用户 ID）
     * @return 更新后的卡片信息
     */
    @Operation(
            summary = "更新卡片",
            description = "更新指定卡片的信息。只更新传入的非空字段，未传入的字段保持原值不变。" +
                    "支持更新 statusId 来移动卡片到其他列。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(schema = @Schema(implementation = JobCardDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "卡片不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
    @PostMapping("/update")
    public ApiResponse<JobCardDto> updateCard(
            @Parameter(description = "更新卡片请求参数", required = true)
            @RequestBody UpdateCardRequest request,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        JobCardDto cardDto = jobCardService.updateCard(userId, request);
        return ApiResponse.success(cardDto);
    }

    /**
     * 移动卡片
     * <p>
     * 接口说明：
     * - 将卡片移动到指定的列（变更状态）
     * - 会校验目标列是否属于同一看板
     * </p>
     *
     * @param request     请求参数
     *                    - cardId: 必填，卡片 ID
     *                    - targetStatusId: 必填，目标列 ID
     * @param httpRequest HTTP 请求（用于获取当前用户 ID）
     * @return 移动后的卡片信息
     */
    @Operation(
            summary = "移动卡片",
            description = "将卡片移动到指定的列（变更状态）。会校验目标列是否属于同一看板。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(schema = @Schema(implementation = JobCardDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "卡片或目标列不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
    @PostMapping("/move")
    public ApiResponse<JobCardDto> moveCard(
            @Parameter(description = "移动卡片请求参数", required = true)
            @RequestBody MoveCardRequest request,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        JobCardDto cardDto = jobCardService.moveCard(userId, request);
        return ApiResponse.success(cardDto);
    }

    /**
     * 删除卡片（软删除）
     * <p>
     * 接口说明：
     * - 软删除指定卡片（设置 deleted_at 字段）
     * - 删除后的卡片不会出现在加载看板接口的响应中
     * </p>
     *
     * @param request     请求参数
     *                    - cardId: 必填，卡片 ID
     * @param httpRequest HTTP 请求（用于获取当前用户 ID）
     * @return 成功响应
     */
    @Operation(
            summary = "删除卡片",
            description = "软删除指定卡片。删除后的卡片不会出现在加载看板接口的响应中。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "卡片不存在"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "无权限")
    })
    @PostMapping("/delete")
    public ApiResponse<Void> deleteCard(
            @Parameter(description = "删除卡片请求参数", required = true)
            @RequestBody DeleteCardRequest request,
            HttpServletRequest httpRequest) {
        String userId = getCurrentUserId(httpRequest);
        jobCardService.deleteCard(userId, request);
        return ApiResponse.success(null);
    }
}
