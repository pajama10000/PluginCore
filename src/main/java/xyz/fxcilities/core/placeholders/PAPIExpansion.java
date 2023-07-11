package xyz.fxcilities.core.placeholders

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.entity.Player
import xyz.fxcilities.core.Core
import xyz.fxcilities.core.placeholders.handlers.ExtensionHandler

class PAPIExpansion(private val manager: ExtensionHandler) : PlaceholderExpansion() {

    companion object {
        val expansions: MutableList<PAPIExpansion> = mutableListOf()
    }

    init {
        expansions.add(this)
    }

    override fun getIdentifier(): String {
        return manager.getPrefix()
    }

    override fun getAuthor(): String {
        return Core.getInstance().description.authors.joinToString(", ")
    }

    override fun getVersion(): String {
        return Core.getInstance().pluginVersion
    }

    override fun onPlaceholderRequest(player: Player, params: String): String {
        return manager.onRequest(player, params)
    }
}
