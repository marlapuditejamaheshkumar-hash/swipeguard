package com.azentio.swipeguard.audit;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Append-only by design: the only write method is append(). There is no change or remove method. */
@Repository
public class AuditRepository {

    private final JdbcClient jdbc;

    public AuditRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Joins the caller's transaction, so the event commits or rolls back with the state change. */
    public void append(String actor, String action, String cardRef, UUID alertId,
                       String reason, String beforeState, String afterState) {
        jdbc.sql("""
                INSERT INTO audit_event (occurred_at, actor, action, card_ref, alert_id, reason,
                    before_state, after_state)
                VALUES (:occurredAt, :actor, :action, :cardRef, :alertId, :reason, :before, :after)
                """)
                .param("occurredAt", OffsetDateTime.now(ZoneOffset.UTC))
                .param("actor", actor)
                .param("action", action)
                .param("cardRef", cardRef)
                .param("alertId", alertId)
                .param("reason", reason)
                .param("before", beforeState)
                .param("after", afterState)
                .update();
    }

    /** Oldest first. */
    public List<AuditEvent> findByCard(String cardRef) {
        return jdbc.sql("""
                SELECT id, occurred_at, actor, action, card_ref, alert_id, reason, before_state, after_state
                FROM audit_event WHERE card_ref = :cardRef ORDER BY occurred_at ASC, id ASC
                """)
                .param("cardRef", cardRef)
                .query((rs, rowNum) -> new AuditEvent(
                        rs.getLong("id"),
                        rs.getObject("occurred_at", OffsetDateTime.class),
                        rs.getString("actor"),
                        rs.getString("action"),
                        rs.getString("card_ref"),
                        rs.getObject("alert_id", UUID.class),
                        rs.getString("reason"),
                        rs.getString("before_state"),
                        rs.getString("after_state")))
                .list();
    }
}