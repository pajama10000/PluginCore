package xyz.fxcilities.core.collections.expiringmap

/** Determines how ExpiringMap entries should be expired. */
enum class ExpirationPolicy {
    /** Expires entries based on when they were last accessed */
    ACCESSED,
    /** Expires entries based on when they were created */
    CREATED
}
