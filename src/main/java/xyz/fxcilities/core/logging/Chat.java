package xyz.fxcilities.core.logging

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.fxcilities.core.Core

/** A utility class for sending chat messages to players */
object Chat {

    /**
     * Formats a message
     *
     * @param text String to format with chat colour codes (Replaces {@literal &} with the special character)
     * @return The formatted string
     */
    fun format(text: String): String {
        return ChatColor.translateAlternateColorCodes('&', text)
    }

    /**
     * Send a message to a {@link CommandSender}. Will automatically format using {@link #format(String)}
     *
     * @param showPrefix If the prefix should be sent in the message
     * @param sender Sender to send the message to. {@link Player} can be passed in, and everything else that
     *     {@link CommandSender} (in this case {@link org.bukkit.permissions.ServerOperator})
     * @param text String of text to send
     */
    fun say(showPrefix: Boolean, sender: CommandSender, text: String) {
        val content = StringBuilder()
        if (showPrefix) {
            content.append(Core.getInstance().prefix)
        }
        content.append(text)
        sender.sendMessage(format(content.toString()))
    }

    /**
     * Sends a message to everyone online with a certain permission
     *
     * @param permission The permission string to filter out
     * @param showPrefix If the prefix should be sent in the message
     * @param text Message to be sent.
     */
    fun say(permission: String, showPrefix: Boolean, text: String) {
        val content = StringBuilder()
        if (showPrefix) {
            content.append(Core.getInstance().prefix)
        }
        content.append(text)

        val newText = format(content.toString())

        for (player in Bukkit.getServer().onlinePlayers) {
            if (player.hasPermission(permission)) {
                player.sendMessage(newText)
            }
        }
    }

    /**
     * Sends a message to everyone online with a certain permission
     *
     * @param permission The permission string to filter out
     * @param text Message to be sent.
     */
    fun say(permission: String, text: String) {
        say(permission, true, text)
    }
}
