package com.andrey.directmessages

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents
import org.bukkit.Bukkit
import org.bukkit.SoundCategory
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.util.logging.Level

class DirectMessages : JavaPlugin() {
    @Volatile
    lateinit var settings: Settings
    lateinit var storage: Storage
    lateinit var profiles: Profiles
    lateinit var messenger: Messenger
    lateinit var pmLog: PmLog

    override fun onEnable() {
        saveDefaultConfig()
        settings = Settings(config)
        storage = runCatching { Storage(this, settings.storage) }.getOrElse {
            logger.log(Level.SEVERE, "Could not connect to the database.", it)
            server.pluginManager.disablePlugin(this)
            return
        }
        profiles = Profiles(this)
        messenger = Messenger(this)
        pmLog = PmLog(this)
        server.pluginManager.registerEvents(profiles, this)
        server.pluginManager.registerEvents(messenger, this)
        profiles.loadOnline()
        lifecycleManager.registerEventHandler(LifecycleEvents.COMMANDS) { Commands(this).register(it.registrar()) }
    }

    fun reload() {
        reloadConfig()
        settings = Settings(config)
    }

    fun send(to: CommandSender, key: String, vararg values: Pair<String, String>) {
        if (to is Player && key in ERRORS) play(to, settings.errorSound)
        var text = settings.message(key)
        if (text.isEmpty()) return
        values.forEach { (token, value) -> text = text.replace(token, value) }
        to.sendMessage(Text.parse(text))
    }

    fun play(player: Player, sound: SoundSetting?) {
        if (sound == null) return
        if (Bukkit.isOwnedByCurrentRegion(player)) {
            player.playSound(player, sound.key, SoundCategory.MASTER, sound.volume, sound.pitch)
        } else {
            player.scheduler.run(this, { player.playSound(player, sound.key, SoundCategory.MASTER, sound.volume, sound.pitch) }, null)
        }
    }

    override fun onDisable() {
        if (::pmLog.isInitialized) pmLog.close()
        if (::storage.isInitialized) storage.close()
    }

    companion object {
        const val SPY_PERMISSION = "directmessages.socialspy"
        const val COOLDOWN_BYPASS = "directmessages.cooldown.bypass"
        const val RELOAD_PERMISSION = "directmessages.reload"
        private val ERRORS = setOf(
            "msg-usage", "reply-usage", "not-online", "no-reply", "self", "not-accepting",
            "you-ignore", "cooldown", "ignore-self", "never-joined"
        )
    }
}
