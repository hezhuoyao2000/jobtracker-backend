package com.example.myfirstspringboot.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.UUID;

/**
 * 职位卡片信息响应
 */
@Data
@Schema(description = "职位卡片信息")
public class JobCardDto {

    @Schema(description = "卡片 ID", example = "550e8400-e29b-41d4-a716-446655440010")
    private UUID id;

    @Schema(description = "所属看板 ID", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID boardId;

    @Schema(description = "所属列 ID（状态）", example = "550e8400-e29b-41d4-a716-446655440001")
    private UUID statusId;

    @Schema(description = "职位名称", example = "高级 Java 开发工程师")
    private String jobTitle;

    @Schema(description = "公司名称", example = "阿里巴巴")
    private String companyName;

    @Schema(description = "职位链接", example = "https://www.example.com/job/123")
    private String jobLink;

    @Schema(description = "来源平台", example = "Boss直聘")
    private String sourcePlatform;

    @Schema(description = "职位是否已过期", example = "false")
    private Boolean expired;

    @Schema(description = "工作地点", example = "杭州")
    private String jobLocation;

    @Schema(description = "职位描述")
    private String description;

    @Schema(description = "申请时间", example = "2026-03-01T10:30:00Z")
    private String appliedTime;

    @Schema(description = "标签，逗号分隔", example = "急招,大厂,高薪")
    private String tags;

    @Schema(description = "备注")
    private String comments;

    @Schema(description = "扩展字段，JSON 字符串")
    private String extra;

    @Schema(description = "创建时间", example = "2026-03-01T10:30:00Z")
    private String createdAt;

    @Schema(description = "更新时间", example = "2026-03-07T08:00:00Z")
    private String updatedAt;
}
