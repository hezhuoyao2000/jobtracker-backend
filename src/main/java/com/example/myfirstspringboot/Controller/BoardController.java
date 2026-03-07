package com.example.myfirstspringboot.Controller;

import com.example.myfirstspringboot.dto.request.CreateBoardRequest;
import com.example.myfirstspringboot.dto.request.LoadBoardRequest;
import com.example.myfirstspringboot.dto.response.BoardDataDto;
import com.example.myfirstspringboot.dto.response.BoardDto;
import com.example.myfirstspringboot.exception.ApiResponse;
import com.example.myfirstspringboot.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Board 控制器
 * <p>
 * 处理看板相关的 HTTP 请求，包括创建看板、加载看板数据等接口
 * </p>
 *
 * @author yourname
 * @since 2026-03-07
 */
@RestController
@RequestMapping("/board")
@RequiredArgsConstructor
@Tag(name = "看板管理", description = "看板相关接口：创建看板、加载看板数据等")
public class BoardController {

    private final BoardService boardService;

    // ========== 接口定义 ==========

    /**
     * 加载看板完整数据
     * <p>
     * 接口说明：
     * - 如果不传 boardId，返回用户的第一个看板
     * - 如果传了 boardId，校验该看板属于当前用户后返回
     * - 返回数据包含：看板信息、所有列、所有卡片（未删除的）
     * </p>
     *
     * @param request 请求参数
     *                - boardId: 可选，看板 ID（UUID 格式），不传则返回第一个看板
     * @return 看板完整数据，包含 board、columns、cards
     *
     * @apiNote 当前 userId 写死为 "user-1"，后续从 JWT Token 中提取
     */
    @Operation(
            summary = "加载看板完整数据",
            description = "加载指定看板的完整数据，包括看板信息、所有列和卡片。" +
                    "如果不传 boardId，返回用户的第一个看板；" +
                    "如果传了 boardId，校验该看板属于当前用户后返回。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(schema = @Schema(implementation = BoardDataDto.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "看板不存在")
    })
    @PostMapping("/load")
    public ApiResponse<BoardDataDto> loadBoard(
            @Parameter(description = "加载看板请求参数", required = true)
            @RequestBody LoadBoardRequest request) {
        // TODO: 后续从 JWT Header 中提取 userId
        // 例如：@RequestHeader("Authorization") String token
        // String userId = jwtUtil.parseUserId(token);
        String userId = "user-1";

        BoardDataDto data = boardService.loadBoard(userId, request);
        return ApiResponse.success(data);
    }

    /**
     * 创建看板
     * <p>
     * 接口说明：
     * - 创建新的看板，并自动初始化 5 个默认列
     * - 默认列：Wish list → Applied → Interviewing → Offered → Rejected
     * - 看板名称为空时，使用默认名称 "My Job Tracker"
     * </p>
     *
     * @param request 请求参数
     *                - name: 可选，看板名称，为空则使用默认名称
     * @return 创建后的看板信息
     *
     * @apiNote 当前 userId 写死为 "user-1"，后续从 JWT Token 中提取
     */
    @Operation(
            summary = "创建看板",
            description = "创建新的看板，并自动初始化 5 个默认列（Wish list、Applied、Interviewing、Offered、Rejected）。"
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功",
                    content = @Content(schema = @Schema(implementation = BoardDto.class)))
    })
    @PostMapping("/create")
    public ApiResponse<BoardDto> createBoard(
            @Parameter(description = "创建看板请求参数", required = true)
            @RequestBody CreateBoardRequest request) {
        // TODO: 后续从 JWT Header 中提取 userId
        String userId = "user-1";

        BoardDto boardDto = boardService.createBoard(userId, request);
        return ApiResponse.success(boardDto);
    }
}
