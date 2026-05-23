package com.pulse.pulse_api.domain.repository;

import com.pulse.pulse_api.domain.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {

    Optional<NotificationTemplate> findByAppIdAndTemplateId(UUID appId, String templateId);
}
