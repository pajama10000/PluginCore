package xyz.fxcilities.core.command

import org.bukkit.command.CommandSender
import org.bukkit.command.defaults.BukkitCommand
import org.bukkit.entity.Player
import xyz.fxcilities.core.Core
import xyz.fxcilities.core.collections.expiringmap.ExpiringMap
import xyz.fxcilities.core.logging.Chat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Represents a command for a server
 *
 * Example:
 *
 * ```
 * class MyCommand : ServerCommand("hello", "says hello world", "/hello", true, listOf("helloworld", "world")) {
 *
 *     init {
 *         setCooldownDuration(5, TimeUnit.SECONDS) // Five second cooldown, this line is optional.
 *     }
 *
 *     override fun onCommand() {
 *         say("This is my command!") // Sends a message to the player
 *         say(false, "&aHello world!") // false to not show the prefix of the plugin. See Core.getPrefix()
 *     }
 * }
 * ```
 */
abstract class ServerCommand(
    label: String,
    description: String,
    usage: String,
    private val playerOnly: Boolean,
    aliases: List<String> = emptyList()
) : BukkitCommand(label, description, usage, aliases) {

    protected lateinit var sender: CommandSender
    protected lateinit var args: Array<String>

    val subCommands: MutableList<ServerSubCommand> = mutableListOf()
    val tabCompleteArgs: MutableList<String> = mutableListOf()

    private var cooldownDuration: Long = 30
    private var cooldownTimeUnit: TimeUnit = TimeUnit.SECONDS

    private val cooldownMap: ExpiringMap<UUID, Long> =
        ExpiringMap.builder().expiration(cooldownDuration, cooldownTimeUnit).build()

    /**
     * Register a sub command
     *
     * @param subCommand The sub command
     */
    fun registerSub(subCommand: ServerSubCommand) {
        subCommands.add(subCommand)
    }

    /**
     * This is an abstract function that is called whenever the command is run. Must be overridden.
     */
    abstract fun onCommand()

    fun setCooldownDuration(duration: Long, timeUnit: TimeUnit) {
        cooldownDuration = duration
        cooldownTimeUnit = timeUnit
        cooldownMap.setExpiration(cooldownDuration, cooldownTimeUnit)
    }

    /**
     * Add arguments for tab completion.
     *
     * @param args The arguments to add
     */
    fun addTabCompleteArgs(vararg args: String) {
        tabCompleteArgs.addAll(args)
    }

    private fun addPrefix(message: String): String {
        return message.replace("{PREFIX}", Core.getInstance().prefix)
    }

    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        populate(sender, args)

        if (playerOnly && sender !is Player) {
            return returnSay(false, addPrefix(Core.instance.notAPlayerMessage))
        }

        if (cooldownDuration > 0 && isPlayer()) {
            val player = sender as Player

            val lastCommandRun = cooldownMap.getOrDefault(player.uniqueId, 0L)
            val difference = cooldownTimeUnit.convert(
                (System.currentTimeMillis() - lastCommandRun), TimeUnit.MILLISECONDS
            )
            if (difference <= cooldownDuration) {
                val remainingTime = (cooldownDuration - difference).toString() + formattedTimeUnit(cooldownTimeUnit)
                return returnSay(false, addPrefix(Core.instance.onCooldownMessage).replace("{TIME}", remainingTime))
            }

            cooldownMap.put(player.uniqueId, System.currentTimeMillis())
        }

        if (args.size >= 1) {
            for (subCommand in subCommands) {
                if (subCommand.label.equals(args[0], ignoreCase = true) ||
                    subCommand.aliases.contains(args[0].toLowerCase())
                ) {
                    subCommand.onCommand()
                    return true
                }
            }
        }

        onCommand()
        return true
    }

    override fun tabComplete(
        sender: CommandSender,
        alias: String,
        args: Array<out String>
    ): List<String>? {
        if (sender !is Player) return null
        populate(sender, args)

        if (args.size == 1) {
            val tabComplete: MutableList<String> = ArrayList()
            for (subCommand in subCommands) {
                if (subCommand.label.startsWith(args[0]) || subCommand.aliases.contains(args[0])) {
                    tabComplete.add(subCommand.label)
                }
                if (subCommand.label.equals(args[0], ignoreCase = true)) {
                    tabComplete.addAll(subCommand.tabCompleteArgs)
                }
            }

            tabComplete.add(label)
            tabComplete.addAll(tabCompleteArgs)

            return tabComplete
        }
        return emptyList()
    }

    /**
     * Send a message to the player
     *
     * @param withPrefix If the prefix should be sent before the message
     * @param message The message to send to the player
     */
    protected fun say(withPrefix: Boolean, message: String) {
        Chat.say(withPrefix, sender, message)
    }

    protected fun say(message: String) {
        say(true, message)
    }

    protected fun isPlayer(): Boolean {
        return sender is Player
    }

    fun getSender(): CommandSender {
        return sender
    }

    private fun returnSay(withPrefix: Boolean, message: String): Boolean {
        say(withPrefix, message)
        return true // To avoid the usage message being sent
    }

    private fun populate(sender: CommandSender, args: Array<out String>) {
        this.sender = sender
        this.args = args
    }

    private fun formattedTimeUnit(unit: TimeUnit): String {
        return when (unit) {
            TimeUnit.HOURS, TimeUnit.DAYS, TimeUnit.MINUTES, TimeUnit.SECONDS -> unit.toString().substring(0, 1).toLowerCase()
            TimeUnit.MILLISECONDS -> "ms"
            TimeUnit.MICROSECONDS -> "micros"
            TimeUnit.NANOSECONDS -> "ns"
            else -> ""
        }
    }
}
