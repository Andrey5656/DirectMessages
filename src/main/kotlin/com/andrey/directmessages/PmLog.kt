package com.andrey.directmessages

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class PmLog(private val plugin: JavaPlugin) {
    private val folder = File(plugin.dataFolder, "logs")
    private val executor = Executors.newSingleThreadExecutor { Thread(it, "DirectMessages Log") }
    private val day = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val time = DateTimeFormatter.ofPattern("HH:mm:ss")

    fun write(sender: String, receiver: String, message: String) {
        val now = LocalDateTime.now()
        executor.execute {
            runCatching {
                folder.mkdirs()
                File(folder, "${day.format(now)}.log").appendText("[${time.format(now)}] $sender -> $receiver: $message${System.lineSeparator()}")
            }.onFailure { plugin.logger.log(Level.WARNING, "Could not write the message log", it) }
        }
    }

    fun close() {
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)
    }
}
