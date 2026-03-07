package com.example.myfirstspringboot.Entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "job_card")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobCard {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "board_id", nullable = false)
    private UUID boardId;

    @Column(name = "status_id", nullable = false)
    private UUID statusId;

    @Column(name = "job_title", nullable = false)
    private String jobTitle;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "job_link")
    private String jobLink;

    @Column(name = "source_platform")
    private String sourcePlatform;

    private Boolean expired;

    @Column(name = "job_location")
    private String jobLocation;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "applied_time")
    private Instant appliedTime;

    @Column(columnDefinition = "text[]")
    private List<String> tags;

    @Column(columnDefinition = "TEXT")
    private String comments;

    /**
     * 扩展字段，存储为 PostgreSQL JSONB 类型
     * 用于存储任意额外的职位信息
     */
    @Column(columnDefinition = "jsonb")
    @Type(JsonBinaryType.class)
    private Map<String, Object> extra;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
