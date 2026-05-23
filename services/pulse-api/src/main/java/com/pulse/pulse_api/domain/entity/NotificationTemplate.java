package com.pulse.pulse_api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notification_templates")
public class NotificationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private App app;

    @Column(name = "template_id", nullable = false)
    private String templateId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "primary_channel", nullable = false)
    private String primaryChannel;

    @Column(name = "fallback_channel")
    private String fallbackChannel;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "definition", columnDefinition = "jsonb", nullable = false)
    private String definition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "retry_config", columnDefinition = "jsonb")
    private String retryConfig;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
