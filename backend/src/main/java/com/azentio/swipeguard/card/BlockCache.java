package com.azentio.swipeguard.card;

import java.util.Optional;

/** Fast copy of the block state. PostgreSQL stays the source of truth. */
public interface BlockCache {

    /** Empty means "not cached": the caller must ask the database. */
    Optional<Boolean> get(String cardRef);

    void put(String cardRef, boolean blocked);
}