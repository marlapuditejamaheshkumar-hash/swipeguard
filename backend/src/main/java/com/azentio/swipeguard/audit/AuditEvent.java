package com.azentio.swipeguard.audit;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditEvent(
        long id,
        OffsetDateTime occurredAt,
        String actor,
        String action,
        String cardRef,
        UUID alertId,
        String reason,
        String beforeState,
        String afterState) { }