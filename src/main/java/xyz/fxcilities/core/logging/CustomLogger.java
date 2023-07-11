package xyz.fxcilities.core.logging

import xyz.fxcilities.core.Core

/**
 * A custom logger
 *
 * @see Core.console
 */
class CustomLogger(private val plugin: Core) {
    private val logger: BukkitLoggerOverride = BukkitLoggerOverride(plugin)

    /**
     * Prints a message to console
     *
     * @param prefix If the prefix should be sent before the text
     * @param text The text to log
     */
    fun print(prefix: Boolean, text: String) {
        val logRecord = StringBuilder()
        if (prefix) {
            logRecord.append(plugin.prefix)
        }
        logRecord.append(text)
        logger.info(Chat.format(logRecord.toString()))
    }

    /**
     * Prints a message to console
     *
     * @param text The text to log
     */
    fun print(text: String) {
        print(false, text)
    }

    /**
     * @return The Core plugin the logger belongs to
     */
    fun getPlugin(): Core {
        return plugin
    }
}
