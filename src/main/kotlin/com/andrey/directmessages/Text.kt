package com.andrey.directmessages

import me.clip.placeholderapi.PlaceholderAPI
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object Text {
    private const val SENDER = ''
    private const val RECEIVER = ''
    private const val MESSAGE = ''

    private val legacy = LegacyComponentSerializer.builder()
        .character('&')
        .hexCharacter('#')
        .hexColors()
        .build()

    fun parse(raw: String): Component = legacy.deserialize(raw.replace('§', '&'))

    fun papi(player: Player, raw: String): String {
        if (raw.indexOf('%') < 0 || !Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) return raw
        return PlaceholderAPI.setPlaceholders(player, raw)
    }

    fun lines(lines: List<Component>): Component =
        Component.textOfChildren(*lines.flatMapIndexed { index, line -> if (index == 0) listOf(line) else listOf(Component.newline(), line) }.toTypedArray())

    fun format(raw: String, sender: Component, receiver: Component, message: String): Component {
        val marked = raw
            .replace("{sender}", SENDER.toString())
            .replace("{receiver}", RECEIVER.toString())
            .replace("{message}", MESSAGE.toString())
        return fill(parse(marked), mapOf(SENDER to sender, RECEIVER to receiver, MESSAGE to Component.text(message)))
    }

    private fun fill(component: Component, tokens: Map<Char, Component>): Component {
        val children = component.children().map { fill(it, tokens) }
        if (component !is TextComponent || component.content().none { it in tokens }) return component.children(children)
        val parts = ArrayList<Component>()
        val text = StringBuilder()
        for (char in component.content()) {
            val token = tokens[char]
            if (token == null) {
                text.append(char)
                continue
            }
            if (text.isNotEmpty()) {
                parts.add(Component.text(text.toString()))
                text.setLength(0)
            }
            parts.add(token)
        }
        if (text.isNotEmpty()) parts.add(Component.text(text.toString()))
        return component.content("").children(parts + children)
    }
}
