package com.fulfillx.analytics.infrastructure.persistence;

import com.fulfillx.analytics.domain.OpsAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OpsAlertRepository extends JpaRepository<OpsAlert, UUID> {
    List<OpsAlert> findByOpenTrueOrderByCreatedAtDesc();

    Optional<OpsAlert> findFirstByCodeAndOpenTrue(String code);
}
