package xyz.fxcilities.core

import com.google.common.base.Charsets
import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import xyz.fxcilities.core.command.ServerCommand
import xyz.fxcilities.core.logging.CustomLogger
import xyz.fxcilities.core.placeholders.PAPIExpansion
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.reflect.Field
import java.util.*
import java.util.concurrent.TimeUnit

abstract class Core : JavaPlugin(), Global {

    companion object {
        lateinit var console: CustomLogger
        lateinit var instance: Core
    }

    var notAPlayerMessage = "{PREFIX}&c&lYou must be a player to run this command!"
    var onCooldownMessage = "{PREFIX}&cYou are on a cooldown! You may run this command again in &l{TIME}"

    val commands = ArrayList<ServerCommand>()

    override fun onEnable() {
        console = CustomLogger(this)
        instance = this

        onPluginEnable()
        val commandMap: CommandMap

        try {
            val bukkitCommandMap: Field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
            bukkitCommandMap.isAccessible = true
            commandMap = bukkitCommandMap.get(Bukkit.getServer()) as CommandMap
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        for (command in commands) {
            commandMap.register(command.label, command)
            console.print(true, "Registered command /${command.label}")
        }

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            console.print(true, "Found PlaceholderAPI, registering placeholders")
            for (expansion in PAPIExpansion.expansions) {
                expansion.register()
            }
        }
    }

    override fun onDisable() {
        onPluginDisable()
    }

    abstract fun onPluginEnable()
    abstract fun onPluginDisable()

    abstract fun getPrefix(): String
    abstract fun getPluginVersion(): String
    abstract fun getPluginName(): String
    abstract fun getPluginAuthors(): Array<String>

    fun setNotAPlayerMessage(message: String) {
        notAPlayerMessage = message
    }

    fun setOnCooldownMessage(message: String) {
        onCooldownMessage = message
    }

    fun loadConfig(fileName: String): FileConfiguration {
        Objects.requireNonNull(fileName, "The fileName argument")

        val config = YamlConfiguration.loadConfiguration(File(dataFolder, fileName))
        saveResource(fileName, false)
        val stream: InputStream? = getResource(fileName)

        stream?.let {
            config.setDefaults(
                YamlConfiguration.loadConfiguration(InputStreamReader(stream, Charsets.UTF_8))
            )
            config.options().copyDefaults(true)
        }

        return config
    }

    companion object {
        fun getInstance(): Core {
            return instance
        }
    }
}
