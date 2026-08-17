package com.fulfillx.order.infrastructure.persistence;

import com.fulfillx.order.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {
    Optional<IdempotencyKey> findByActorIdAndKey(UUID actorId, String key);
}
