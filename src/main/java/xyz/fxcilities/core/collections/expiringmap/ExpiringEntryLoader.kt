package xyz.fxcilities.core.collections.expiringmap

/**
 * Loads entries on demand, with control over each value's expiry duration (i.e. variable
 * expiration).
 *
 * @param K Key type
 * @param V Value type
 */
interface ExpiringEntryLoader<K, V> {
    /**
     * Called to load a new value for the [key] into an expiring map.
     *
     * @param key to load a value for
     * @return contains new value to load along with its expiry duration
     */
    fun load(key: K): ExpiringValue<V>
}
