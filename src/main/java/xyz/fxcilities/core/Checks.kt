package xyz.fxcilities.core

import java.util.NoSuchElementException

object Checks {
    /**
     * @param obj The object to check if null
     * @param name The name of the object (Example: "player")
     * @return Returns the obj parameter if not null
     * @throws IllegalArgumentException if the object is null
     */
    fun <T> nonNull(obj: T?, name: String): T {
        if (obj == null) {
            throw IllegalArgumentException("$name cannot be null.")
        }
        return obj
    }

    /**
     * A way of doing assertions
     *
     * @param failed If the check failed
     * @param name Name of the check
     * @throws RuntimeException if failed was true
     */
    fun check(failed: Boolean, name: String) {
        if (failed) {
            throw RuntimeException(name)
        }
    }

    /**
     * Taken from ExpiringMap by Jonathan Halterman
     */
    fun state(expression: Boolean, errorMessageFormat: String, vararg args: Any) {
        if (!expression) {
            throw IllegalStateException(String.format(errorMessageFormat, *args))
        }
    }

    /**
     * Taken from ExpiringMap by Jonathan Halterman
     */
    fun element(element: Any?, key: Any) {
        if (element == null) {
            throw NoSuchElementException(key.toString())
        }
    }
}
