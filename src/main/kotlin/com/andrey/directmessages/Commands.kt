package com.andrey.directmessages

import io.papermc.paper.command.brigadier.BasicCommand
import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Bukkit
import org.bukkit.entity.Player

class Commands(private val plugin: DirectMessages) {
    fun register(registrar: io.papermc.paper.command.brigadier.Commands) {
        registrar.register("msg", "Send a private message.", listOf("tell", "whisper", "message", "w"), Msg())
        registrar.register("r", "Reply to your last private message.", listOf("reply"), Reply())
        registrar.register("ignore", "Ignore a player, or list the players you ignore.", listOf("block"), Ignore())
        registrar.register("msgtoggle", "Turn receiving private messages on or off.", emptyList(), MsgToggle())
        registrar.register("socialspy", "See other players' private messages.", emptyList(), SocialSpy())
        registrar.register("directmessages", "Reload DirectMessages.", emptyList(), Reload())
    }

    private fun player(source: CommandSourceStack): Player? {
        val player = source.executor as? Player ?: source.sender as? Player
        if (player == null) source.sender.sendMessage("Only players can use this command.")
        return player
    }

    private fun onThread(player: Player, task: () -> Unit) {
        if (Bukkit.isOwnedByCurrentRegion(player)) task() else player.scheduler.run(plugin, { task() }, null)
    }

    private fun names(source: CommandSourceStack, prefix: String): List<String> {
        val viewer = source.executor as? Player ?: source.sender as? Player
        return Bukkit.getOnlinePlayers()
            .filter { it != viewer && (viewer == null || viewer.canSee(it)) && it.name.startsWith(prefix, ignoreCase = true) }
            .map { it.name }
    }

    private inner class Msg : BasicCommand {
        override fun execute(source: CommandSourceStack, args: Array<String>) {
            val player = player(source) ?: return
            val message = args.drop(1).joinToString(" ")
            if (message.isBlank()) return plugin.send(player, "msg-usage")
            onThread(player) { plugin.messenger.msg(player, args[0], message) }
        }

        override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> =
            if (args.size <= 1) names(source, args.firstOrNull() ?: "") else emptyList()
    }

    private inner class Reply : BasicCommand {
        override fun execute(source: CommandSourceStack, args: Array<String>) {
            val player = player(source) ?: return
            val message = args.joinToString(" ")
            if (message.isBlank()) return plugin.send(player, "reply-usage")
            onThread(player) { plugin.messenger.reply(player, message) }
        }
    }

    private inner class Ignore : BasicCommand {
        override fun execute(source: CommandSourceStack, args: Array<String>) {
            val player = player(source) ?: return
            val ignores = plugin.profiles.get(player.uniqueId).ignores
            val name = args.firstOrNull() ?: ""
            if (name.isEmpty()) {
                if (ignores.isEmpty()) return plugin.send(player, "ignore-empty")
                return plugin.send(player, "ignore-list", "{players}" to ignores.values.sortedBy { it.lowercase() }.joinToString(", "))
            }
            val target = ignores.entries.firstOrNull { it.value.equals(name, ignoreCase = true) }?.let { it.key to it.value }
                ?: Bukkit.getOfflinePlayerIfCached(name)?.let { it.uniqueId to (it.name ?: name) }
                ?: return plugin.send(player, "never-joined", "{player}" to name)
            if (target.first == player.uniqueId) return plugin.send(player, "ignore-self")
            val on = plugin.profiles.toggleIgnore(player.uniqueId, target.first, target.second)
            plugin.send(player, if (on) "ignore-on" else "ignore-off", "{player}" to target.second)
        }

        override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> {
            if (args.size > 1) return emptyList()
            val prefix = args.firstOrNull() ?: ""
            val player = source.executor as? Player ?: source.sender as? Player ?: return names(source, prefix)
            val ignored = plugin.profiles.get(player.uniqueId).ignores.values.filter { it.startsWith(prefix, ignoreCase = true) }
            return (names(source, prefix) + ignored).distinctBy { it.lowercase() }
        }
    }

    private inner class MsgToggle : BasicCommand {
        override fun execute(source: CommandSourceStack, args: Array<String>) {
            val player = player(source) ?: return
            plugin.send(player, if (plugin.profiles.toggleMsgOff(player.uniqueId)) "toggle-off" else "toggle-on")
        }
    }

    private inner class SocialSpy : BasicCommand {
        override fun execute(source: CommandSourceStack, args: Array<String>) {
            val player = player(source) ?: return
            plugin.send(player, if (plugin.profiles.toggleSpy(player.uniqueId)) "spy-on" else "spy-off")
        }

        override fun permission() = DirectMessages.SPY_PERMISSION
    }

    private inner class Reload : BasicCommand {
        override fun execute(source: CommandSourceStack, args: Array<String>) {
            if (!args.firstOrNull().equals("reload", ignoreCase = true)) {
                source.sender.sendMessage("Usage: /directmessages reload")
                return
            }
            plugin.reload()
            plugin.send(source.sender, "reloaded")
        }

        override fun suggest(source: CommandSourceStack, args: Array<String>): Collection<String> =
            if (args.size <= 1 && "reload".startsWith(args.firstOrNull() ?: "", ignoreCase = true)) listOf("reload") else emptyList()

        override fun permission() = DirectMessages.RELOAD_PERMISSION
    }
}
