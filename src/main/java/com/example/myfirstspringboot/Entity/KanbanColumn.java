package com.example.myfirstspringboot.Entity;

import jakarta.persistence.*;
import lombok.*;

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
     * 自定义属性，以 JSON 字符串存储（简化处理）
     * 如需复杂操作可在 Service 层序列化/反序列化
     */
    @Column(name = "custom_attributes", columnDefinition = "TEXT")
    private String customAttributes;

}
