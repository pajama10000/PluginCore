package xyz.fxcilities.core.placeholders.handlers

import org.bukkit.entity.Player

/**
 * @see xyz.fxcilities.core.placeholders.PAPIExpansion
 */
interface ExtensionHandler {
    /**
     * Called when a placeholder request is made
     *
     * @param player The player that requested the placeholder
     * @param placeholder The placeholder that was requested, prefix not included
     * @return The result of the request
     */
    fun onRequest(player: Player, placeholder: String): String

    /**
     * @return The prefix of the extension
     */
    fun getPrefix(): String
}
