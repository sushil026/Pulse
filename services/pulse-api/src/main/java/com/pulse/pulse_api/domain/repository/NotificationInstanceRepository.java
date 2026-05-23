package com.pulse.pulse_api.domain.repository;

import com.pulse.pulse_api.domain.entity.NotificationInstance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationInstanceRepository extends JpaRepository<NotificationInstance, UUID> {

    Optional<NotificationInstance> findByAppIdAndInstanceId(UUID appId, String instanceId);
}
