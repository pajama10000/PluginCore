package xyz.fxcilities.core.logging

import org.bukkit.plugin.Plugin
import java.util.logging.Level
import java.util.logging.Logger

/**
 * A replacement for Bukkit's Logger to remove their default formatting
 *
 * @see org.bukkit.plugin.PluginLogger
 */
class BukkitLoggerOverride(context: Plugin) : Logger(
    context.javaClass.canonicalName,
    null
) {
    init {
        parent = context.server.logger
        level = Level.ALL
    }
}
