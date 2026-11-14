package com.azentio.swipeguard.velocity;

import java.time.Duration;
import java.time.Instant;

public interface VelocityStore {

    /**
     * Records this authorisation and returns how many authorisations the card has in the window.
     * The window is measured from the REQUEST timestamp, not the server clock.
     * Using transactionId as the member makes a repeated call a no-op.
     */
    long recordAndCount(String cardRef, String transactionId, Instant at, Duration window);

    /** Current number of entries held for the card (used by tests). */
    long size(String cardRef);
}