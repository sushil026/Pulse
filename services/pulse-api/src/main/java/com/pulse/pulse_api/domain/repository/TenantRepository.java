package com.pulse.pulse_api.domain.repository;

import com.pulse.pulse_api.domain.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TenantRepository extends JpaRepository<Tenant, UUID> {
}
