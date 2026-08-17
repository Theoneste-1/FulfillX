package com.fulfillx.analytics.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ops_alerts")
public class OpsAlert {
    @Id
    private UUID id;

    @Column(nullable = false, length = 16)
    private String severity;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(nullable = false)
    private boolean open;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    protected OpsAlert() {
    }

    public static OpsAlert open(String severity, String code, String message) {
        OpsAlert alert = new OpsAlert();
        alert.id = UUID.randomUUID();
        alert.severity = severity;
        alert.code = code;
        alert.message = message;
        alert.open = true;
        alert.createdAt = Instant.now();
        return alert;
    }

    public void resolve() {
        this.open = false;
        this.resolvedAt = Instant.now();
    }

    public void refresh(String severity, String message) {
        this.severity = severity;
        this.message = message;
    }

    public UUID getId() {
        return id;
    }

    public String getSeverity() {
        return severity;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public boolean isOpen() {
        return open;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }
}
