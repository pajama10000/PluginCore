package xyz.fxcilities.core.collections.expiringmap

/**
 * A listener for expired object events.
 *
 * @param K Key type
 * @param V Value type
 */
fun interface ExpirationListener<K, V> {
    /**
     * Called when a map entry expires.
     *
     * @param key Expired key
     * @param value Expired value
     */
    fun expired(key: K, value: V)
}
