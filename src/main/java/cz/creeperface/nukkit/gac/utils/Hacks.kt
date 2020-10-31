package cz.creeperface.nukkit.gac.utils

import cn.nukkit.Server
import cn.nukkit.network.AdvancedSourceInterface
import cn.nukkit.network.Network
import cn.nukkit.network.RakNetInterface
import cz.creeperface.nukkit.gac.network.GACNetworkInterface

private val networkAdvancedInterfaces by lazy {
    Network::class.accessProperty("advancedInterfaces").get(Server.getInstance().network) as MutableSet<AdvancedSourceInterface>
}

fun Network.injectGACInterface() {
    val raknet = this.interfaces.find { it is RakNetInterface } as? RakNetInterface ?: return

    this.interfaces.remove(raknet)
    networkAdvancedInterfaces.remove(raknet)

    val gacInterface = GACNetworkInterface(server, raknet)
    this.interfaces.add(gacInterface)
    networkAdvancedInterfaces.add(gacInterface)
}