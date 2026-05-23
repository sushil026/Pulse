package com.pulse.pulse_api.domain.repository;

import com.pulse.pulse_api.domain.entity.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByPublicKeyAndActiveTrue(String publicKey);
}
