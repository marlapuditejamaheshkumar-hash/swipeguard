package com.azentio.swipeguard.risk;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Every threshold, score and list comes from application.yml (swipeguard.risk.*). */
@ConfigurationProperties(prefix = "swipeguard.risk")
public record RiskProperties(
        BigDecimal highAmountThreshold,
        int highAmountScore,
        List<String> riskyMcc,
        int riskyMccScore,
        int crossBorderScore,
        String homeCountry,
        int velocityCount,
        Duration velocityWindow,
        int velocityScore,
        int declineThreshold,
        int reviewThreshold) {
}