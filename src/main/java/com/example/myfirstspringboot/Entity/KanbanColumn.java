package com.example.myfirstspringboot.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.util.UUID;

/**
 * 看板列实体类
 * 用于存储看板中的状态列（如：Wish list, Applied, Interviewing 等）
 */
@TableName("kanban_column")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KanbanColumn {
    @TableId(type = IdType.INPUT)
    private UUID id;

    @TableField("board_id")
    private UUID boardId;

    @TableField("name")
    private String name;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("is_default")
    private Boolean isDefault;

    /**
     * 自定义属性，以 JSON 字符串存储（简化处理）
     * 如需复杂操作可在 Service 层序列化/反序列化
     */
    @TableField("custom_attributes")
    private String customAttributes;
}
