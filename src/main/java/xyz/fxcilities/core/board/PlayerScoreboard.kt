package xyz.fxcilities.core.board

import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.scoreboard.DisplaySlot
import org.bukkit.scoreboard.Objective
import org.bukkit.scoreboard.Scoreboard

import java.util.ArrayList
import java.util.List

/**
 * Scoreboard wrapper
 *
 * Example:
 * ```
 * val board = PlayerScoreboard("&aServer Name")
 * board.addLine("&bRank: &c&lOwner")
 * board.addLine("Name: " + player.name)
 * board.addBlankSpace() // Blank line
 * board.addLine("play.server.com")
 *
 * player.scoreboard = board.scoreboard
 * ```
 */
class PlayerScoreboard(title: String) {
    companion object {
        const val MAX_LINES = 16
    }

    private val scoreboard: Scoreboard = Bukkit.getScoreboardManager().newScoreboard
    private val objective: Objective = scoreboard.registerNewObjective(title, "dummy")

    private val modifies: MutableList<String> = ArrayList(MAX_LINES)

    init {
        objective.displayName = title
        objective.displaySlot = DisplaySlot.SIDEBAR
    }

    /** Sets the scoreboard title. */
    fun setTitle(title: String) {
        objective.displayName = title
    }

    /** Modifies the line with §r strings in the way to add a line equal to another. */
    private fun getLineCoded(line: String): String {
        var result = line
        while (modifies.contains(result)) result += ChatColor.RESET
        return result.substring(0, Math.min(40, result.length))
    }

    /** Adds a new line to the scoreboard. Throw an error if the lines count are higher than 16. */
    fun addLine(line: String) {
        if (modifies.size > MAX_LINES) {
            throw IndexOutOfBoundsException("You cannot add more than 16 lines.")
        }
        val modified = getLineCoded(line)
        modifies.add(modified)
        objective.getScore(modified).score = -(modifies.size)
    }

    /** Adds a blank space to the scoreboard. */
    fun addBlankSpace() {
        addLine(" ")
    }

    /** Sets a scoreboard line to an exact index (between 0 and 15). */
    fun setLine(index: Int, line: String) {
        if (index < 0 || index >= MAX_LINES) {
            throw IndexOutOfBoundsException("The index cannot be negative or higher than 15.")
        }
        val oldModified = modifies[index]
        scoreboard.resetScores(oldModified)
        val modified = getLineCoded(line)
        modifies[index] = modified
        objective.getScore(modified).score = -(index + 1)
    }

    /** Gets the Bukkit Scoreboard. */
    fun getScoreboard(): Scoreboard {
        return scoreboard
    }

    /** Just for debug. */
    override fun toString(): String {
        var out = ""
        var i = 0
        for (string in modifies) {
            out += "${-(i + 1)})-> $string;\n"
        }
        return out
    }
}
