package com.example.myfirstspringboot.Entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.Instant;

/**
 * 用户实体类
 */
@TableName("users")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @TableId(type = IdType.INPUT)
    private String id;

    @TableField("username")
    private String username;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("email")
    private String email;

    @TableField("display_name")
    private String displayName;

    @TableField("avatar_url")
    private String avatarUrl;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private Instant createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private Instant updatedAt;
}
