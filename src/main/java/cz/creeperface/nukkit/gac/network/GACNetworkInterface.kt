package cz.creeperface.nukkit.gac.network

import cn.nukkit.Player
import cn.nukkit.Server
import cn.nukkit.event.player.PlayerCreationEvent
import cn.nukkit.network.AdvancedSourceInterface
import cn.nukkit.network.RakNetInterface
import cn.nukkit.utils.MainLogger
import com.nukkitx.network.raknet.RakNetServer
import com.nukkitx.network.raknet.RakNetServerListener
import com.nukkitx.network.raknet.RakNetSessionListener
import cz.creeperface.nukkit.gac.player.ICheatPlayer
import cz.creeperface.nukkit.gac.utils.accessProperty
import cz.creeperface.nukkit.gac.utils.gacPlayerClass
import cz.creeperface.nukkit.gac.utils.getValue
import org.joor.Reflect
import java.lang.reflect.InvocationTargetException
import java.net.InetSocketAddress
import java.util.*
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmName

class GACNetworkInterface(private val server: Server, private val delegate: RakNetInterface) :
    RakNetServerListener by delegate, AdvancedSourceInterface by delegate {

    private val sessionCreationQueue: Queue<RakNetSessionListener> by lazy {
        RakNetInterface::class.accessProperty("sessionCreationQueue").getValue(delegate)
    }

    init {
        val raknet: RakNetServer = RakNetInterface::class.accessProperty("raknet").getValue(delegate)
        raknet.listener = this
    }

    override fun process(): Boolean {
        while (sessionCreationQueue.isNotEmpty()) {
            val session = sessionCreationQueue.poll()

            val address: InetSocketAddress = Reflect.on(session).field("raknet").call("getAddress").get()
            val ev = PlayerCreationEvent(this, Player::class.java, Player::class.java, null, address)
            server.pluginManager.callEvent(ev)

            var clazz = ev.playerClass.kotlin

            if (!clazz.isSubclassOf(ICheatPlayer::class)) {
                MainLogger.getLogger()
                    .error("Another plugin tried to extend Player class with (${clazz.jvmName}) which would conflict with GAC. Please contact the plugin author")
                clazz = gacPlayerClass
            }

            try {
                val player: Player = Reflect.onClass(clazz.java).create(
                    this, ev.clientId, ev.socketAddress
                ).get()
                server.addPlayer(address, player)

                Reflect.on(session).set("player", player)

                val sessions: MutableMap<InetSocketAddress, RakNetSessionListener> =
                    Reflect.on(delegate).get("sessions")
                sessions[address] = session
            } catch (e: NoSuchMethodException) {
                Server.getInstance().logger.logException(e)
            } catch (e: InvocationTargetException) {
                Server.getInstance().logger.logException(e)
            } catch (e: InstantiationException) {
                Server.getInstance().logger.logException(e)
            } catch (e: IllegalAccessException) {
                Server.getInstance().logger.logException(e)
            }
        }

        return delegate.process()
    }
}