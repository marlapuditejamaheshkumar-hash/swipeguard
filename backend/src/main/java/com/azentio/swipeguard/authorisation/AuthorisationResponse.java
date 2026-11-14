package com.azentio.swipeguard.authorisation;

import com.azentio.swipeguard.risk.Decision;
import com.azentio.swipeguard.risk.Reason;
import java.util.List;
import java.util.UUID;

public record AuthorisationResponse(
        String transactionId,
        Decision decision,
        int score,
        List<Reason> reasons,
        UUID alertId,
        String maskedPan,
        String cardRef) { }