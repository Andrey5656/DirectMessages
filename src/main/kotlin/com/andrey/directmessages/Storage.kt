package com.andrey.directmessages

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.sql.Connection
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.logging.Level

class Storage(private val plugin: JavaPlugin, section: ConfigurationSection) {
    private val mysql = section.getString("type", "sqlite").equals("mysql", ignoreCase = true)

    private val source = HikariDataSource(HikariConfig().apply {
        poolName = "DirectMessages"
        if (mysql) {
            driverClassName = "com.mysql.cj.jdbc.Driver"
            jdbcUrl = "jdbc:mysql://${section.getString("mysql.host", "localhost")}:${section.getInt("mysql.port", 3306)}/${section.getString("mysql.database", "minecraft")}"
            username = section.getString("mysql.username", "root")
            password = section.getString("mysql.password", "")
            maximumPoolSize = 4
        } else {
            driverClassName = "org.sqlite.JDBC"
            jdbcUrl = "jdbc:sqlite:" + File(plugin.dataFolder, "data.db").absolutePath
            maximumPoolSize = 1
        }
    })

    private val executor = Executors.newSingleThreadExecutor { Thread(it, "DirectMessages Storage") }

    init {
        source.connection.use { connection ->
            connection.createStatement().use {
                it.executeUpdate("CREATE TABLE IF NOT EXISTS dm_ignores (player VARCHAR(36) NOT NULL, ignored VARCHAR(36) NOT NULL, name VARCHAR(64) NOT NULL, PRIMARY KEY (player, ignored))")
                it.executeUpdate("CREATE TABLE IF NOT EXISTS dm_socialspy (player VARCHAR(36) PRIMARY KEY)")
                it.executeUpdate("CREATE TABLE IF NOT EXISTS dm_msgoff (player VARCHAR(36) PRIMARY KEY)")
            }
        }
    }

    fun load(uuid: UUID, done: (Profile) -> Unit) {
        executor.execute { done(query(uuid)) }
    }

    fun loadNow(uuid: UUID): Profile = executor.submit(Callable { query(uuid) }).get()

    private fun query(uuid: UUID): Profile = runCatching {
        source.connection.use { connection ->
            val ignores = ConcurrentHashMap<UUID, String>()
            connection.prepareStatement("SELECT ignored, name FROM dm_ignores WHERE player = ?").use { statement ->
                statement.setString(1, uuid.toString())
                statement.executeQuery().use { result ->
                    while (result.next()) ignores[UUID.fromString(result.getString(1))] = result.getString(2)
                }
            }
            Profile(ignores, exists(connection, "dm_socialspy", uuid), exists(connection, "dm_msgoff", uuid))
        }
    }.getOrElse {
        plugin.logger.log(Level.WARNING, "Could not load the data of $uuid", it)
        Profile(ConcurrentHashMap(), false, false)
    }

    private fun exists(connection: Connection, table: String, uuid: UUID): Boolean =
        connection.prepareStatement("SELECT 1 FROM $table WHERE player = ?").use { statement ->
            statement.setString(1, uuid.toString())
            statement.executeQuery().use { it.next() }
        }

    fun addIgnore(player: UUID, ignored: UUID, name: String) =
        update("REPLACE INTO dm_ignores (player, ignored, name) VALUES (?, ?, ?)", player.toString(), ignored.toString(), name)

    fun removeIgnore(player: UUID, ignored: UUID) =
        update("DELETE FROM dm_ignores WHERE player = ? AND ignored = ?", player.toString(), ignored.toString())

    fun setSpy(player: UUID, on: Boolean) = flag("dm_socialspy", player, on)

    fun setMsgOff(player: UUID, off: Boolean) = flag("dm_msgoff", player, off)

    private fun flag(table: String, player: UUID, on: Boolean) =
        if (on) update("REPLACE INTO $table (player) VALUES (?)", player.toString())
        else update("DELETE FROM $table WHERE player = ?", player.toString())

    private fun update(sql: String, vararg values: String) {
        executor.execute {
            runCatching {
                source.connection.use { connection ->
                    connection.prepareStatement(sql).use { statement ->
                        values.forEachIndexed { index, value -> statement.setString(index + 1, value) }
                        statement.executeUpdate()
                    }
                }
            }.onFailure { plugin.logger.log(Level.WARNING, "Could not save to the database", it) }
        }
    }

    fun close() {
        executor.shutdown()
        executor.awaitTermination(10, TimeUnit.SECONDS)
        source.close()
    }
}
