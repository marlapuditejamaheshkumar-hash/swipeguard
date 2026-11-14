package com.azentio.swipeguard.card;

import com.azentio.swipeguard.audit.AuditRepository;
import com.azentio.swipeguard.common.BadRequestException;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class CardControlService {

    private static final String ACTIVE = "ACTIVE";
    private static final String BLOCKED = "BLOCKED";

    private final CardControlRepository repository;
    private final AuditRepository auditRepository;
    private final BlockCache cache;

    public CardControlService(CardControlRepository repository, AuditRepository auditRepository, BlockCache cache) {
        this.repository = repository;
        this.auditRepository = auditRepository;
        this.cache = cache;
    }

    @Transactional
    public void block(String cardRef, String actor, String reason, UUID alertId) {
        change(cardRef, actor, reason, alertId, true);
    }

    @Transactional
    public void unblock(String cardRef, String actor, String reason, UUID alertId) {
        change(cardRef, actor, reason, alertId, false);
    }

    /** Check path: Redis first, database on a miss, then the cache is filled. */
    public boolean isBlocked(String cardRef) {
        return cache.get(cardRef).orElseGet(() -> {
            boolean blocked = repository.isBlocked(cardRef);
            cache.put(cardRef, blocked);
            return blocked;
        });
    }

    /** State row, alert status and audit event are written in ONE transaction. */
    private void change(String cardRef, String actor, String reason, UUID alertId, boolean blocked) {
        boolean wasBlocked = repository.isBlocked(cardRef);
        repository.saveState(cardRef, blocked, reason, actor);

        if (alertId != null) {
            String alertStatus = blocked ? "CARD_BLOCKED" : "OPEN";
            if (repository.setAlertStatus(alertId, alertStatus) == 0) {
                throw new BadRequestException("alertId", "no alert with this id"); // rolls everything back
            }
        }
        auditRepository.append(actor, blocked ? "BLOCK" : "UNBLOCK", cardRef, alertId, reason,
                label(wasBlocked), label(blocked));

        // Write-through to Redis only after the database commit succeeded.
        afterCommit(() -> cache.put(cardRef, blocked));
    }

    private static String label(boolean blocked) {
        return blocked ? BLOCKED : ACTIVE;
    }

    private static void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
        } else {
            action.run();
        }
    }
}