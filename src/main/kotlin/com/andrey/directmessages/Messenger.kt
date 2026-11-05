package com.andrey.directmessages

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class Reply(val target: UUID, val name: String, val incoming: Boolean)

class Messenger(private val plugin: DirectMessages) : Listener {
    private val replies = ConcurrentHashMap<UUID, Reply>()
    private val lastSent = ConcurrentHashMap<UUID, Long>()

    fun msg(sender: Player, name: String, message: String) {
        val target = plugin.server.getPlayerExact(name)
        if (target == null || !sender.canSee(target)) return plugin.send(sender, "not-online", "{player}" to name)
        send(sender, target, message)
    }

    fun reply(sender: Player, message: String) {
        val reply = replies[sender.uniqueId] ?: return plugin.send(sender, "no-reply")
        val target = plugin.server.getPlayer(reply.target)
        if (target == null || !(reply.incoming || sender.canSee(target))) return plugin.send(sender, "not-online", "{player}" to reply.name)
        send(sender, target, message)
    }

    private fun send(sender: Player, target: Player, message: String) {
        if (target == sender) return plugin.send(sender, "self")
        if (plugin.profiles.ignores(sender.uniqueId, target.uniqueId)) return plugin.send(sender, "you-ignore", "{player}" to target.name)
        if (plugin.profiles.ignores(target.uniqueId, sender.uniqueId) || plugin.profiles.msgOff(target.uniqueId)) {
            return plugin.send(sender, "not-accepting", "{player}" to target.name)
        }
        val settings = plugin.settings
        if (settings.cooldownMillis > 0 && !sender.hasPermission(DirectMessages.COOLDOWN_BYPASS)) {
            val now = System.currentTimeMillis()
            val left = (lastSent[sender.uniqueId] ?: 0) + settings.cooldownMillis - now
            if (left > 0) return plugin.send(sender, "cooldown", "{seconds}" to String.format(Locale.ROOT, "%.1f", left / 1000.0))
            lastSent[sender.uniqueId] = now
        }
        replies[sender.uniqueId] = Reply(target.uniqueId, target.name, false)
        val senderCard = card(sender, settings)
        val scheduled = target.scheduler.run(plugin, { deliver(sender, target, senderCard, message, settings) }) {
            plugin.send(sender, "not-online", "{player}" to target.name)
        }
        if (scheduled == null) plugin.send(sender, "not-online", "{player}" to target.name)
    }

    private fun deliver(sender: Player, target: Player, senderCard: Component, message: String, settings: Settings) {
        val receiverCard = card(target, settings)
        replies[target.uniqueId] = Reply(sender.uniqueId, sender.name, true)
        if (settings.received.isNotEmpty()) target.sendMessage(Text.format(settings.received, senderCard, receiverCard, message))
        if (settings.actionBar.isNotEmpty()) target.sendActionBar(Text.format(settings.actionBar, senderCard, receiverCard, message))
        plugin.play(target, settings.receivedSound)
        if (settings.spy.isNotEmpty()) {
            val line = Text.format(settings.spy, senderCard, receiverCard, message)
            plugin.server.onlinePlayers.forEach {
                if (it != sender && it != target && plugin.profiles.spy(it.uniqueId) && it.hasPermission(DirectMessages.SPY_PERMISSION)) it.sendMessage(line)
            }
        }
        if (settings.log) plugin.pmLog.write(sender.name, target.name, message)
        sender.scheduler.run(plugin, {
            if (settings.sent.isNotEmpty()) sender.sendMessage(Text.format(settings.sent, senderCard, receiverCard, message))
            plugin.play(sender, settings.sentSound)
        }, null)
    }

    private fun card(player: Player, settings: Settings): Component {
        var card = Text.parse(Text.papi(player, settings.name.replace("{name}", player.name)))
        if (settings.hoverLines.isNotEmpty()) {
            val lines = settings.hoverLines.map { Text.parse(Text.papi(player, it.replace("{name}", player.name))) }
            card = card.hoverEvent(HoverEvent.showText(Text.lines(lines)))
        }
        if (settings.click.isNotEmpty()) card = card.clickEvent(ClickEvent.suggestCommand(settings.click.replace("{name}", player.name)))
        return card
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onCommand(event: PlayerCommandPreprocessEvent) {
        val label = event.message.substringBefore(' ')
        if (label.lowercase() !in VANILLA) return
        event.isCancelled = true
        event.player.performCommand("msg" + event.message.substring(label.length))
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        replies.remove(event.player.uniqueId)
        lastSent.remove(event.player.uniqueId)
    }

    companion object {
        private val VANILLA = setOf("/minecraft:msg", "/minecraft:tell", "/minecraft:w")
    }
}
