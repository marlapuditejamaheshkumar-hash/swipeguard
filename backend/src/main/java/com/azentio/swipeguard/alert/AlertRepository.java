package com.azentio.swipeguard.alert;

import com.azentio.swipeguard.authorisation.AuthorisationRequest;
import com.azentio.swipeguard.authorisation.AuthorisationResponse;
import com.azentio.swipeguard.risk.Decision;
import com.azentio.swipeguard.risk.ReasonCodec;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AlertRepository {

    private static final String COLUMNS = """
            id, transaction_id, card_ref, masked_pan, amount, currency, mcc, merchant_country,
            decision, score, reasons, status, created_at
            """;

    private final JdbcClient jdbc;
    private final ReasonCodec codec;

    public AlertRepository(JdbcClient jdbc, ReasonCodec codec) {
        this.jdbc = jdbc;
        this.codec = codec;
    }

    public void insert(AuthorisationRequest request, AuthorisationResponse response) {
        jdbc.sql("""
                INSERT INTO alert (id, transaction_id, card_ref, masked_pan, amount, currency, mcc,
                    merchant_country, decision, score, reasons, status, created_at)
                VALUES (:id, :transactionId, :cardRef, :maskedPan, :amount, :currency, :mcc,
                    :country, :decision, :score, :reasons, :status, :createdAt)
                """)
                .param("id", response.alertId())
                .param("transactionId", response.transactionId())
                .param("cardRef", response.cardRef())
                .param("maskedPan", response.maskedPan())
                .param("amount", request.amount())
                .param("currency", request.currency())
                .param("mcc", request.mcc())
                .param("country", request.merchantCountry())
                .param("decision", response.decision().name())
                .param("score", response.score())
                .param("reasons", codec.toJson(response.reasons()))
                .param("status", AlertStatus.OPEN.name())
                .param("createdAt", OffsetDateTime.now(ZoneOffset.UTC))
                .update();
    }

    /** Newest first. Uses idx_alert_status_created_at when a status filter is given. */
    public List<AlertView> find(AlertStatus status, int page, int size) {
        String where = status == null ? "" : " WHERE status = :status";
        JdbcClient.StatementSpec spec = jdbc.sql("SELECT " + COLUMNS + " FROM alert" + where
                + " ORDER BY created_at DESC, id LIMIT :size OFFSET :offset")
                .param("size", size)
                .param("offset", page * size);
        if (status != null) {
            spec = spec.param("status", status.name());
        }
        return spec.query((rs, rowNum) -> new AlertView(
                rs.getObject("id", java.util.UUID.class),
                rs.getString("transaction_id"),
                rs.getString("card_ref"),
                rs.getString("masked_pan"),
                rs.getBigDecimal("amount"),
                rs.getString("currency"),
                rs.getString("mcc"),
                rs.getString("merchant_country"),
                Decision.valueOf(rs.getString("decision")),
                rs.getInt("score"),
                codec.fromJson(rs.getString("reasons")),
                AlertStatus.valueOf(rs.getString("status")),
                rs.getObject("created_at", OffsetDateTime.class))).list();
    }

    public long count(AlertStatus status) {
        if (status == null) {
            return jdbc.sql("SELECT COUNT(*) FROM alert").query(Long.class).single();
        }
        return jdbc.sql("SELECT COUNT(*) FROM alert WHERE status = :status")
                .param("status", status.name()).query(Long.class).single();
    }
}