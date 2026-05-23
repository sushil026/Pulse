package com.pulse.pulse_api.domain.repository;

import com.pulse.pulse_api.domain.entity.App;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AppRepository extends JpaRepository<App, UUID> {
}
