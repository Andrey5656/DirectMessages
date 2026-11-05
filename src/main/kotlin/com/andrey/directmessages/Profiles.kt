package com.andrey.directmessages

import io.papermc.paper.event.player.AsyncChatEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class Profile(val ignores: MutableMap<UUID, String>, @Volatile var spy: Boolean, @Volatile var msgOff: Boolean)

class Profiles(private val plugin: DirectMessages) : Listener {
    private val profiles = ConcurrentHashMap<UUID, Profile>()

    fun get(uuid: UUID): Profile = profiles.getOrPut(uuid) { Profile(ConcurrentHashMap(), false, false) }

    fun ignores(player: UUID, other: UUID) = profiles[player]?.ignores?.containsKey(other) == true

    fun spy(player: UUID) = profiles[player]?.spy == true

    fun msgOff(player: UUID) = profiles[player]?.msgOff == true

    fun loadOnline() = plugin.server.onlinePlayers.forEach { load(it.uniqueId) }

    private fun load(uuid: UUID) = plugin.storage.load(uuid) { profiles[uuid] = it }

    fun toggleIgnore(player: UUID, other: UUID, name: String): Boolean {
        val ignores = get(player).ignores
        if (ignores.remove(other) != null) {
            plugin.storage.removeIgnore(player, other)
            return false
        }
        ignores[other] = name
        plugin.storage.addIgnore(player, other, name)
        return true
    }

    fun toggleSpy(player: UUID): Boolean {
        val profile = get(player)
        profile.spy = !profile.spy
        plugin.storage.setSpy(player, profile.spy)
        return profile.spy
    }

    fun toggleMsgOff(player: UUID): Boolean {
        val profile = get(player)
        profile.msgOff = !profile.msgOff
        plugin.storage.setMsgOff(player, profile.msgOff)
        return profile.msgOff
    }

    @EventHandler(priority = EventPriority.MONITOR)
    fun onPreLogin(event: AsyncPlayerPreLoginEvent) {
        if (event.loginResult == AsyncPlayerPreLoginEvent.Result.ALLOWED) profiles[event.uniqueId] = plugin.storage.loadNow(event.uniqueId)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        if (!profiles.containsKey(event.player.uniqueId)) load(event.player.uniqueId)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        profiles.remove(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    fun onChat(event: AsyncChatEvent) {
        val speaker = event.player.uniqueId
        event.viewers().removeIf { it is Player && ignores(it.uniqueId, speaker) }
    }
}
