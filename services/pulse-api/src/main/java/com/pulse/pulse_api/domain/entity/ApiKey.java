package com.pulse.pulse_api.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = false)
    private App app;

    @Column(name = "public_key", nullable = false, unique = true)
    private String publicKey;

    @Column(name = "secret_key", nullable = false, unique = true)
    private String secretKey;

    @Column(name = "active")
    private boolean active;

    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
