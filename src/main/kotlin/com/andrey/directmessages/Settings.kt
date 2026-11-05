package com.andrey.directmessages

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.FileConfiguration

class SoundSetting(val key: String, val volume: Float, val pitch: Float)

class Settings(private val config: FileConfiguration) {
    val name = string("name")
    val sent = string("formats.sent")
    val received = string("formats.received")
    val spy = string("formats.spy")
    val actionBar = string("formats.action-bar")
    val hoverLines: List<String> = config.getStringList("hover.lines")
    val click = string("hover.click")
    val sentSound = sound("sounds.sent")
    val receivedSound = sound("sounds.received")
    val errorSound = sound("sounds.error")
    val cooldownMillis = (config.getDouble("cooldown") * 1000).toLong().coerceAtLeast(0)
    val log = config.getBoolean("log")
    val storage: ConfigurationSection = config.getConfigurationSection("storage") ?: config.createSection("storage")

    fun message(key: String) = string("messages.$key")

    private fun string(path: String) = config.getString(path) ?: ""

    private fun sound(path: String): SoundSetting? {
        val key = string("$path.sound")
        if (key.isEmpty()) return null
        return SoundSetting(key, config.getDouble("$path.volume").toFloat(), config.getDouble("$path.pitch").toFloat())
    }
}
