package cz.creeperface.nukkit.gac.network

import cn.nukkit.Player
import cn.nukkit.Server
import cn.nukkit.event.player.PlayerCreationEvent
import cn.nukkit.network.AdvancedSourceInterface
import cn.nukkit.network.RakNetInterface
import cn.nukkit.network.session.RakNetPlayerSession
import cn.nukkit.utils.MainLogger
import com.nukkitx.network.raknet.RakNetServer
import com.nukkitx.network.raknet.RakNetServerListener
import com.nukkitx.network.raknet.RakNetServerSession
import cz.creeperface.nukkit.gac.player.ICheatPlayer
import cz.creeperface.nukkit.gac.utils.accessProperty
import cz.creeperface.nukkit.gac.utils.gacPlayerClass
import cz.creeperface.nukkit.gac.utils.getValue
import org.joor.Reflect
import java.net.InetSocketAddress
import java.util.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmName

class GACNetworkInterface(private val server: Server, private val delegate: RakNetInterface) :
    RakNetServerListener by delegate, AdvancedSourceInterface by delegate {

    private val sessionCreationQueue: Queue<RakNetPlayerSession> by lazy {
        RakNetInterface::class.accessProperty("sessionCreationQueue").getValue(delegate)
    }

    private val sessions: MutableMap<InetSocketAddress, RakNetPlayerSession> by lazy {
        RakNetInterface::class.accessProperty("sessions").getValue(delegate)
    }

    init {
        val raknet: RakNetServer = RakNetInterface::class.accessProperty("raknet").getValue(delegate)
        raknet.listener = this
    }

    override fun process(): Boolean {
        while (sessionCreationQueue.isNotEmpty()) {
            val session = sessionCreationQueue.poll()

            val address: InetSocketAddress = session.rakNetSession.address

            try {
                val ev = PlayerCreationEvent(this, Player::class.java, Player::class.java, null, address)
                server.pluginManager.callEvent(ev)

                var clazz = ev.playerClass.kotlin

                if (!clazz.isSubclassOf(ICheatPlayer::class)) {
                    MainLogger.getLogger()
                        .error("Another plugin tried to extend Player class with (${clazz.jvmName}) which would conflict with GAC. Please contact the plugin author")
                    clazz = gacPlayerClass
                }

                sessions[ev.socketAddress] = session

                val player: Player = Reflect.onClass(clazz.java).create(
                    this, ev.clientId, ev.socketAddress
                ).get()
                server.addPlayer(address, player)

                session.player = player
            } catch (e: Exception) {
                server.logger.error("Failed to create player", e)
                session.disconnect("Internal error")
                sessions.remove(address)
            }
        }

        val iterator = this.sessions.values.iterator()
        while (iterator.hasNext()) {
            val nukkitSession = iterator.next()
            val player = nukkitSession.player
            if (nukkitSession.disconnectReason != null) {
                player.close(player.leaveMessage, nukkitSession.disconnectReason, false)
                iterator.remove()
            } else {
                nukkitSession.serverTick()
            }
        }

        return true
    }

    // force delegate default methods
    override fun onSessionCreation(p0: RakNetServerSession) {
        delegate.onSessionCreation(p0)
    }

    override fun onConnectionRequest(address: InetSocketAddress, realAddress: InetSocketAddress): Boolean {
        return delegate.onConnectionRequest(address, realAddress)
    }
}