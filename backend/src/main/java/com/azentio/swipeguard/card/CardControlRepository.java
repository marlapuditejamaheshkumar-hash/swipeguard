package com.azentio.swipeguard.card;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class CardControlRepository {

    private final JdbcClient jdbc;

    public CardControlRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public boolean isBlocked(String cardRef) {
        return jdbc.sql("SELECT blocked FROM card_block WHERE card_ref = :cardRef")
                .param("cardRef", cardRef)
                .query(Boolean.class)
                .optional()
                .orElse(false);
    }

    /** Update the row if it exists, otherwise insert it (works on PostgreSQL and H2). */
    public void saveState(String cardRef, boolean blocked, String reason, String actor) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        int updated = jdbc.sql("""
                UPDATE card_block SET blocked = :blocked, reason = :reason, updated_by = :actor,
                    updated_at = :now WHERE card_ref = :cardRef
                """)
                .param("blocked", blocked).param("reason", reason).param("actor", actor)
                .param("now", now).param("cardRef", cardRef)
                .update();
        if (updated == 0) {
            jdbc.sql("""
                    INSERT INTO card_block (card_ref, blocked, reason, updated_by, updated_at)
                    VALUES (:cardRef, :blocked, :reason, :actor, :now)
                    """)
                    .param("cardRef", cardRef).param("blocked", blocked).param("reason", reason)
                    .param("actor", actor).param("now", now)
                    .update();
        }
    }

    /** Returns how many alert rows changed (0 means the alert id does not exist). */
    public int setAlertStatus(UUID alertId, String status) {
        return jdbc.sql("UPDATE alert SET status = :status WHERE id = :id")
                .param("status", status).param("id", alertId)
                .update();
    }
}