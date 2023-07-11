package xyz.fxcilities.core.command

/**
 * Represents a sub command for a [ServerCommand]
 *
 * Example:
 *
 * ```
 * class MySubCommand(parent: ServerCommand) : ServerSubCommand(parent, "world", "my fun subcommand", "/hello world", listOf("planet", "earth")) {
 *
 *     override fun onCommand() {
 *         say("Hello world!")
 *     }
 * }
 * ```
 */
abstract class ServerSubCommand(
    val parent: ServerCommand,
    val label: String,
    val description: String,
    val usage: String,
    val aliases: List<String>
) {

    val tabCompleteArgs: MutableList<String> = mutableListOf()

    init {
        parent.registerSub(this)
    }

    abstract fun onCommand()

    /**
     * Adds arguments for tab completion.
     *
     * @param args The arguments to add
     * @see ServerCommand.addTabCompleteArgs
     */
    fun addTabCompleteArgs(vararg args: String) {
        tabCompleteArgs.addAll(args)
    }

    /**
     * @see ServerCommand.say
     */
    protected fun say(withPrefix: Boolean, message: String) {
        parent.say(withPrefix, message)
    }

    /**
     * @see ServerCommand.say
     */
    protected fun say(message: String) {
        parent.say(message)
    }
}
