package xyz.fxcilities.core.collections.expiringmap

import java.util.concurrent.TimeUnit

/**
 * A value which should be stored in an [ExpiringMap] with optional control over its expiration.
 *
 * @param V the type of value being stored
 */
class ExpiringValue<V> private constructor(
    val value: V,
    val expirationPolicy: ExpirationPolicy?,
    val duration: Long,
    val timeUnit: TimeUnit?
) {
    companion object {
        private const val UNSET_DURATION = -1L
    }

    /**
     * Creates an ExpiringValue to be stored in an [ExpiringMap]. The map's default values for
     * [ExpirationPolicy] and [ExpiringMap.getExpiration] expiration} will be used.
     *
     * @param value the value to store
     * @see ExpiringMap.put
     */
    constructor(value: V) : this(value, null, UNSET_DURATION, null)

    /**
     * Creates an ExpiringValue to be stored in an [ExpiringMap]. The map's default [ExpiringMap.getExpiration] expiration} will be used.
     *
     * @param value the value to store
     * @param expirationPolicy the expiration policy for the value
     * @see ExpiringMap.put
     */
    constructor(value: V, expirationPolicy: ExpirationPolicy) : this(value, null, UNSET_DURATION, expirationPolicy)

    /**
     * Creates an ExpiringValue to be stored in an [ExpiringMap]. The map's default [ExpirationPolicy] will be used.
     *
     * @param value the value to store
     * @param duration the length of time after an entry is created that it should be removed
     * @param timeUnit the unit that [duration] is expressed in
     * @see ExpiringMap.put
     * @throws NullPointerException on null timeUnit
     */
    constructor(value: V, duration: Long, timeUnit: TimeUnit) : this(value, null, duration, timeUnit) {
        requireNotNull(timeUnit)
    }

    /**
     * Creates an ExpiringValue to be stored in an [ExpiringMap].
     *
     * @param value the value to store
     * @param duration the length of time after an entry is created that it should be removed
     * @param timeUnit the unit that [duration] is expressed in
     * @param expirationPolicy the expiration policy for the value
     * @see ExpiringMap.put
     * @throws NullPointerException on null timeUnit
     */
    constructor(value: V, expirationPolicy: ExpirationPolicy, duration: Long, timeUnit: TimeUnit) : this(value, expirationPolicy, duration, timeUnit) {
        requireNotNull(timeUnit)
    }

    override fun hashCode(): Int {
        return value?.hashCode() ?: 0
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is ExpiringValue<*>) {
            return false
        }
        return value == other.value && expirationPolicy == other.expirationPolicy &&
                duration == other.duration && timeUnit == other.timeUnit
    }

    override fun toString(): String {
        return "ExpiringValue(value=$value, expirationPolicy=$expirationPolicy, duration=$duration, timeUnit=$timeUnit)"
    }
}
