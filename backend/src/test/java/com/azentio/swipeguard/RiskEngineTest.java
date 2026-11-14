package com.azentio.swipeguard;

import static org.assertj.core.api.Assertions.assertThat;

import com.azentio.swipeguard.authorisation.AuthorisationRequest;
import com.azentio.swipeguard.authorisation.Channel;
import com.azentio.swipeguard.risk.Decision;
import com.azentio.swipeguard.risk.Reason;
import com.azentio.swipeguard.risk.RiskEngine;
import com.azentio.swipeguard.risk.RiskProperties;
import com.azentio.swipeguard.risk.RiskResult;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class RiskEngineTest {

    private final RiskEngine engine = new RiskEngine(new RiskProperties(
            new BigDecimal("50000"), 40, List.of("7995", "6051", "4829"), 30,
            30, "IN", 6, Duration.ofMinutes(10), 50, 80, 40));

    private AuthorisationRequest request(String amount, String mcc, String country, Channel channel) {
        return new AuthorisationRequest("t1", "4111111111111111", new BigDecimal(amount), "INR",
                mcc, country, channel, Instant.parse("2026-11-14T06:00:00Z"));
    }

    private List<String> rules(RiskResult result) {
        return result.reasons().stream().map(Reason::rule).toList();
    }

    @Test
    void highAmountFiresOnlyAboveThreshold() {
        assertThat(rules(engine.evaluate(request("50000.01", "5411", "IN", Channel.POS), 1))).contains("HIGH_AMOUNT");
        assertThat(rules(engine.evaluate(request("50000", "5411", "IN", Channel.POS), 1))).doesNotContain("HIGH_AMOUNT");
    }

    @Test
    void riskyMccFiresOnlyForListedCodes() {
        assertThat(rules(engine.evaluate(request("100", "7995", "IN", Channel.POS), 1))).contains("RISKY_MCC");
        assertThat(rules(engine.evaluate(request("100", "5411", "IN", Channel.POS), 1))).doesNotContain("RISKY_MCC");
    }

    @Test
    void crossBorderFiresOnlyForEcomOutsideIndia() {
        assertThat(rules(engine.evaluate(request("100", "5411", "US", Channel.ECOM), 1))).contains("CROSS_BORDER_ECOM");
        assertThat(rules(engine.evaluate(request("100", "5411", "IN", Channel.ECOM), 1))).doesNotContain("CROSS_BORDER_ECOM");
        assertThat(rules(engine.evaluate(request("100", "5411", "US", Channel.POS), 1))).doesNotContain("CROSS_BORDER_ECOM");
    }

    @Test
    void velocityFiresFromTheSixthAuthorisation() {
        assertThat(rules(engine.evaluate(request("100", "5411", "IN", Channel.POS), 6))).contains("VELOCITY");
        assertThat(rules(engine.evaluate(request("100", "5411", "IN", Channel.POS), 5))).doesNotContain("VELOCITY");
    }

    @Test
    void scoreBelow40Approves() {
        RiskResult result = engine.evaluate(request("100", "5411", "IN", Channel.POS), 1);
        assertThat(result.score()).isZero();
        assertThat(result.decision()).isEqualTo(Decision.APPROVE);
    }

    @Test
    void score40To79Reviews() {
        RiskResult result = engine.evaluate(request("60000", "5411", "IN", Channel.POS), 1);
        assertThat(result.score()).isEqualTo(40);
        assertThat(result.decision()).isEqualTo(Decision.REVIEW);
    }

    @Test
    void score80OrMoreDeclines() {
        RiskResult result = engine.evaluate(request("100", "7995", "IN", Channel.POS), 6); // 30 + 50
        assertThat(result.score()).isEqualTo(80);
        assertThat(result.decision()).isEqualTo(Decision.DECLINE);
    }
}