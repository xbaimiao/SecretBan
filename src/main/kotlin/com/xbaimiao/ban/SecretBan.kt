package com.xbaimiao.ban

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.events.PacketContainer
import com.xbaimiao.easylib.EasyPlugin
import com.xbaimiao.easylib.chat.Lang.sendLang
import com.xbaimiao.easylib.command.ArgNode
import com.xbaimiao.easylib.command.command
import com.xbaimiao.easylib.util.formatTime
import com.xbaimiao.easylib.util.onlinePlayers
import com.xbaimiao.easylib.util.registerListener
import com.xbaimiao.easylib.util.submit
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.concurrent.Executors
import kotlin.random.Random

@Suppress("unused")
class SecretBan : EasyPlugin(), Listener {

    private val thread = Executors.newFixedThreadPool(10)!!
    private val playerArgNode = ArgNode("玩家", exec = { token ->
        onlinePlayers().map { it.name }.filter { it.startsWith(token) }
    }, parse = { token ->
        token
    })

    override fun enable() {
        registerListener(this)
        command<CommandSender>("secretban") {
            permission = "secretban.use"
            subCommand<CommandSender>("ban") {
                description = "隐秘封禁一个玩家"
                val playerArg = arg(playerArgNode)
                val timeArg = times("时间", optional = true)
                exec {
                    val time = timeArg.valueOrNull()
                    val player = playerArg.value()
                    Bukkit.getPlayerExact(player)?.let { secretKickPlayer(it) }
                    SecretBanList.addSecretBan(player, time ?: 0)
                    sender.sendLang("secretban-ban", player, if (time == null) "永久" else formatTime(time))
                }
            }
            subCommand<CommandSender>("unban") {
                description = "取消一个玩家的隐秘封禁"
                val playerArg = arg(playerArgNode)
                exec {
                    val player = playerArg.value()
                    if (SecretBanList.removeSecretBan(player)) {
                        sender.sendLang("secretban-unban", player)
                    } else {
                        sender.sendLang("secretban-not-ban", player)
                    }
                }
            }
        }.register()
    }

    @EventHandler
    fun join(event: PlayerJoinEvent) {
        if (SecretBanList.isSecretBan(event.player.name)) {
            submit(delay = 20) {
                secretKickPlayer(event.player)
            }
        }
    }

    private fun secretKickPlayer(player: Player) {
        thread.submit {
            val start = System.currentTimeMillis()
            while (player.isOnline && System.currentTimeMillis() - start < 1000) {
                try {
                    val packet = PacketContainer(PacketType.Play.Server.EXPLOSION)
                    packet.doubles.write(0, player.location.x.offset())
                    packet.doubles.write(1, player.location.y.offset())
                    packet.doubles.write(2, player.location.z.offset())
                    packet.blockPositionCollectionModifier.write(0, arrayListOf())
                    packet.float.write(0, 100.0f)
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet, null, true)
                } catch (e: Throwable) {
                    e.printStackTrace()
                    break
                }
            }
            submit {
                player.kickPlayer("Timed out")
            }
        }
    }

    private fun Double.offset(): Double {
        return if (Random.nextBoolean()) {
            this + Random.nextDouble(10.0)
        } else {
            this - Random.nextDouble(10.0)
        }
    }

}

