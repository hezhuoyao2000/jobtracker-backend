package com.example.myfirstspringboot.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@TableName("board")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Board {
    @TableId(type = IdType.INPUT)
    private UUID id;

    @TableField("user_id")
    private String userId;

    @TableField("name")
    private String name;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}
