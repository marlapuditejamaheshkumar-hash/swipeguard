package com.azentio.swipeguard.authorisation;

import com.azentio.swipeguard.alert.AlertRepository;
import com.azentio.swipeguard.risk.Decision;
import com.azentio.swipeguard.risk.ReasonCodec;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class AuthorisationRepository {

    private final JdbcClient jdbc;
    private final ReasonCodec codec;
    private final AlertRepository alertRepository;

    public AuthorisationRepository(JdbcClient jdbc, ReasonCodec codec, AlertRepository alertRepository) {
        this.jdbc = jdbc;
        this.codec = codec;
        this.alertRepository = alertRepository;
    }

    /** One transaction: the decision row and (for REVIEW/DECLINE) the alert row are saved together. */
    @Transactional
    public void saveDecision(AuthorisationRequest request, AuthorisationResponse response) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        jdbc.sql("""
                INSERT INTO card_authorisation (id, transaction_id, card_ref, masked_pan, amount, currency,
                    mcc, merchant_country, channel, tx_timestamp, decision, score, reasons, alert_id, created_at)
                VALUES (:id, :transactionId, :cardRef, :maskedPan, :amount, :currency,
                    :mcc, :country, :channel, :txTimestamp, :decision, :score, :reasons, :alertId, :createdAt)
                """)
                .param("id", UUID.randomUUID())
                .param("transactionId", response.transactionId())
                .param("cardRef", response.cardRef())
                .param("maskedPan", response.maskedPan())
                .param("amount", request.amount())
                .param("currency", request.currency())
                .param("mcc", request.mcc())
                .param("country", request.merchantCountry())
                .param("channel", request.channel().name())
                .param("txTimestamp", OffsetDateTime.ofInstant(request.timestamp(), ZoneOffset.UTC))
                .param("decision", response.decision().name())
                .param("score", response.score())
                .param("reasons", codec.toJson(response.reasons()))
                .param("alertId", response.alertId())
                .param("createdAt", now)
                .update();

        if (response.alertId() != null) {
            alertRepository.insert(request, response);
        }
    }

    public Optional<AuthorisationResponse> findByTransactionId(String transactionId) {
        return jdbc.sql("""
                SELECT transaction_id, decision, score, reasons, alert_id, masked_pan, card_ref
                FROM card_authorisation WHERE transaction_id = :transactionId
                """)
                .param("transactionId", transactionId)
                .query((rs, rowNum) -> new AuthorisationResponse(
                        rs.getString("transaction_id"),
                        Decision.valueOf(rs.getString("decision")),
                        rs.getInt("score"),
                        codec.fromJson(rs.getString("reasons")),
                        rs.getObject("alert_id", UUID.class),
                        rs.getString("masked_pan"),
                        rs.getString("card_ref")))
                .optional();
    }
}