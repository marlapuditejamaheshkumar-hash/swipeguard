package com.azentio.swipeguard.risk;

import com.azentio.swipeguard.authorisation.AuthorisationRequest;
import com.azentio.swipeguard.authorisation.Channel;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/** Pure scoring logic: no database, no Redis. The velocity count is passed in. */
@Component
public class RiskEngine {

    private final RiskProperties props;

    public RiskEngine(RiskProperties props) {
        this.props = props;
    }

    public RiskResult evaluate(AuthorisationRequest request, long velocityCount) {
        int score = 0;
        List<Reason> reasons = new ArrayList<>();

        if (request.amount().compareTo(props.highAmountThreshold()) > 0) {
            score += props.highAmountScore();
            reasons.add(new Reason("HIGH_AMOUNT",
                    "amount " + request.amount() + " exceeds " + props.highAmountThreshold()));
        }
        if (props.riskyMcc().contains(request.mcc())) {
            score += props.riskyMccScore();
            reasons.add(new Reason("RISKY_MCC", "merchant category " + request.mcc() + " is high risk"));
        }
        if (request.channel() == Channel.ECOM && !props.homeCountry().equals(request.merchantCountry())) {
            score += props.crossBorderScore();
            reasons.add(new Reason("CROSS_BORDER_ECOM",
                    "e-commerce transaction in " + request.merchantCountry()));
        }
        if (velocityCount >= props.velocityCount()) {
            score += props.velocityScore();
            reasons.add(new Reason("VELOCITY",
                    velocityCount + " authorisations within " + props.velocityWindow().toMinutes() + " minutes"));
        }

        return new RiskResult(decide(score), score, reasons);
    }

    private Decision decide(int score) {
        if (score >= props.declineThreshold()) {
            return Decision.DECLINE;
        }
        if (score >= props.reviewThreshold()) {
            return Decision.REVIEW;
        }
        return Decision.APPROVE;
    }
}