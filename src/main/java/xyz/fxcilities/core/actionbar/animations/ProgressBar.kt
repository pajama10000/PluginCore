package xyz.fxcilities.core.actionbar.animations

import org.bukkit.scheduler.BukkitRunnable
import xyz.fxcilities.core.Core
import xyz.fxcilities.core.actionbar.PlayerActionBar

/**
 * Create a progress bar animation using the [PlayerActionBar] wrapper.
 *
 * Example:
 * A progress bar with the parameters:
 * ```
 * ProgressBar(actionBar, 15, 20, "&f> ", "&a|", "&f <")
 * ```
 * would appear as: `> |||||| <`
 */
class ProgressBar(
    private val actionBar: PlayerActionBar,
    private val maxBarTicks: Int,
    private val maxDisplayTicks: Int,
    private val begin: String,
    private val middle: String,
    private val end: String
) : BukkitRunnable() {

    private var ticked = 0
    private var displayTicks = 0

    /** Start the animation */
    override fun run() {
        displayTicks++

        if (displayTicks >= maxDisplayTicks) {
            this.cancel()
            return
        }

        if (ticked < maxBarTicks) {
            ticked++
        }

        val bar = (0 until ticked).map { middle }.joinToString("")

        actionBar.setBar(begin + bar + end)
        actionBar.sendBar()
    }

    init {
        runTaskTimer(Core.getInstance(), 0L, 1L)
    }
}
