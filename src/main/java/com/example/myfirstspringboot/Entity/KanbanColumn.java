package com.example.myfirstspringboot.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.util.Map;
import java.util.UUID;

/**
 * 看板列实体类
 * 用于存储看板中的状态列（如：Wish list, Applied, Interviewing 等）
 */
@Entity
@Table(name = "kanban_column")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KanbanColumn {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(nullable = false)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault;

    /**
     * 自定义属性，存储为 PostgreSQL JSONB 类型
     * 用于扩展列的额外信息
     */
    @Column(name = "custom_attributes", columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private Map<String, Object> customAttributes;

}
