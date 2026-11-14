package com.azentio.swipeguard.alert;

import com.azentio.swipeguard.risk.Decision;
import com.azentio.swipeguard.risk.Reason;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AlertView(
        UUID id,
        String transactionId,
        String cardRef,
        String maskedPan,
        BigDecimal amount,
        String currency,
        String mcc,
        String merchantCountry,
        Decision decision,
        int score,
        List<Reason> reasons,
        AlertStatus status,
        OffsetDateTime createdAt) { }