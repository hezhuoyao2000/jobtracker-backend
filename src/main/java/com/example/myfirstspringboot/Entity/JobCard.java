package com.example.myfirstspringboot.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@TableName("job_card")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCard {
    @TableId(type = IdType.INPUT)
    private UUID id;

    @TableField("board_id")
    private UUID boardId;

    @TableField("status_id")
    private UUID statusId;

    @TableField("job_title")
    private String jobTitle;

    @TableField("company_name")
    private String companyName;

    @TableField("job_link")
    private String jobLink;

    @TableField("source_platform")
    private String sourcePlatform;

    @TableField("expired")
    private Boolean expired;

    @TableField("job_location")
    private String jobLocation;

    @TableField("description")
    private String description;

    @TableField("applied_time")
    private Instant appliedTime;

    @TableField("tags")
    private String tags;

    @TableField("comments")
    private String comments;

    /**
     * 扩展字段，以 JSON 字符串存储（简化处理）
     * 如需复杂操作可在 Service 层序列化/反序列化
     */
    @TableField("extra")
    private String extra;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;

    @TableField("deleted_at")
    @TableLogic(value = "NULL", delval = "NOW()")
    private Instant deletedAt;
}
