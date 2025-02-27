package xyz.fxcilities.core.actionbar

import net.md_5.bungee.api.ChatMessageType
import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.entity.Player
import xyz.fxcilities.core.logging.Chat

/** Represents an action bar for the player */
class PlayerActionBar(private val player: Player) {
    private var content: String? = null

    /**
     * Set the action bar's content
     *
     * @param content The new content of the action bar
     */
    fun setBar(content: String) {
        this.content = content
    }

    /** Send the action bar */
    fun sendBar() {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(Chat.format(content)))
    }

    /**
     * @return The player that the action bar belongs to.
     */
    fun getPlayer(): Player {
        return player
    }
}
