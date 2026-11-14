package com.azentio.swipeguard.authorisation;

import com.azentio.swipeguard.card.CardIdentity;
import com.azentio.swipeguard.risk.Decision;
import com.azentio.swipeguard.risk.RiskEngine;
import com.azentio.swipeguard.risk.RiskProperties;
import com.azentio.swipeguard.risk.RiskResult;
import com.azentio.swipeguard.velocity.VelocityStore;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class AuthorisationService {

    private static final Logger log = LoggerFactory.getLogger(AuthorisationService.class);

    private final CardIdentity cardIdentity;
    private final VelocityStore velocityStore;
    private final RiskEngine riskEngine;
    private final RiskProperties props;
    private final AuthorisationRepository repository;

    public AuthorisationService(CardIdentity cardIdentity, VelocityStore velocityStore, RiskEngine riskEngine,
                                RiskProperties props, AuthorisationRepository repository) {
        this.cardIdentity = cardIdentity;
        this.velocityStore = velocityStore;
        this.riskEngine = riskEngine;
        this.props = props;
        this.repository = repository;
    }

    public AuthorisationResponse check(AuthorisationRequest request) {
        // 1. Replay of a known transactionId: return the stored answer, do NOT touch velocity.
        Optional<AuthorisationResponse> previous = repository.findByTransactionId(request.transactionId());
        if (previous.isPresent()) {
            return previous.get();
        }

        // 2. From here on only cardRef and maskedPan are used, never the PAN.
        String cardRef = cardIdentity.cardRef(request.pan());
        String maskedPan = CardIdentity.mask(request.pan());

        // 3. Count + score.
        long count = velocityStore.recordAndCount(cardRef, request.transactionId(),
                request.timestamp(), props.velocityWindow());
        RiskResult result = riskEngine.evaluate(request, count);

        UUID alertId = result.decision() == Decision.APPROVE ? null : UUID.randomUUID();
        AuthorisationResponse response = new AuthorisationResponse(request.transactionId(),
                result.decision(), result.score(), result.reasons(), alertId, maskedPan, cardRef);

        // 4. Save. If two identical requests race, the unique constraint rejects the loser,
        //    who then returns the winner's stored row.
        try {
            repository.saveDecision(request, response);
        } catch (DuplicateKeyException e) {
            return repository.findByTransactionId(request.transactionId()).orElseThrow();
        }

        log.info("authorisation tx={} card={} decision={} score={}",
                response.transactionId(), cardRef, response.decision(), response.score());
        return response;
    }
}