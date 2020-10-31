package cz.creeperface.nukkit.gac.network

import cn.nukkit.Player
import cn.nukkit.Server
import cn.nukkit.event.player.PlayerCreationEvent
import cn.nukkit.network.AdvancedSourceInterface
import cn.nukkit.network.RakNetInterface
import cn.nukkit.network.SourceInterface
import cn.nukkit.utils.MainLogger
import com.nukkitx.network.raknet.RakNetServer
import com.nukkitx.network.raknet.RakNetServerListener
import com.nukkitx.network.raknet.RakNetServerSession
import com.nukkitx.network.raknet.RakNetSessionListener
import cz.creeperface.nukkit.gac.player.ICheatPlayer
import cz.creeperface.nukkit.gac.utils.accessProperty
import cz.creeperface.nukkit.gac.utils.gacPlayerClass
import cz.creeperface.nukkit.gac.utils.getValue
import java.lang.reflect.InvocationTargetException
import java.net.InetSocketAddress
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmName

class GACNetworkInterface(private val server: Server, private val delegate: RakNetInterface) : RakNetServerListener by delegate, AdvancedSourceInterface by delegate {

    private val sessionListeners: MutableSet<RakNetSessionListener> = RakNetInterface::class.accessProperty("sessionListeners").getValue(delegate)

    private val nukkitListenerFactory = run {
        val constructor = Class.forName("cn.nukkit.network.RakNetInterface\$NukkitSessionListener")
                .declaredConstructors.first()
        constructor.isAccessible = true

        return@run { player: Player ->
            constructor.newInstance(delegate, player) as RakNetSessionListener
        }
    }

    init {
        val raknet: RakNetServer = RakNetInterface::class.accessProperty("raknet").getValue(delegate)
        raknet.listener = this
    }

    override fun onSessionCreation(session: RakNetServerSession) {
        val ev = PlayerCreationEvent(this, gacPlayerClass.java, gacPlayerClass.java, null, session.address)
        server.pluginManager.callEvent(ev)
        var clazz: KClass<*> = ev.playerClass.kotlin

        if (!clazz.isSubclassOf(ICheatPlayer::class)) {
            MainLogger.getLogger().error("Another plugin tried to extend Player class with (${clazz.jvmName}) which would conflict with GAC. Please contact the plugin author")
            clazz = gacPlayerClass
        }

        try {
            val constructor = clazz.java.getConstructor(SourceInterface::class.java, Class.forName("java.lang.Long"), InetSocketAddress::class.java)
            val player = constructor.newInstance(this, ev.clientId, ev.socketAddress) as Player

            server.addPlayer(session.address, player)
            val listener = nukkitListenerFactory(player)
            this.sessionListeners.add(listener)
            session.listener = listener
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
}