package com.pulse.pulse_api.domain.repository;

import com.pulse.pulse_api.domain.entity.Outbox;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<Outbox, UUID> {

    List<Outbox> findTop100ByPublishedFalseOrderByCreatedAtAsc();
}
