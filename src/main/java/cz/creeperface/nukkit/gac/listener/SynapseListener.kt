package cz.creeperface.nukkit.gac.listener

import cn.nukkit.event.EventHandler
import cn.nukkit.event.EventPriority
import cn.nukkit.event.Listener
import cn.nukkit.plugin.Plugin
import cz.creeperface.nukkit.gac.GTAnticheat
import cz.creeperface.nukkit.gac.player.GTSynapseCheatPlayer
import cz.creeperface.nukkit.gac.player.GTSynapseCheatPlayer11
import cz.creeperface.nukkit.gac.player.SynapseCheatPlayer
import cz.creeperface.nukkit.gac.utils.registerEvent
import org.itxtech.synapseapi.SynapsePlayer11
import org.itxtech.synapseapi.event.player.SynapsePlayerCreationEvent

/**
 * @author CreeperFace
 */
class SynapseListener(plugin: Plugin) : Listener {

    init {
        registerEvent(this, plugin, SynapsePlayerCreationEvent::class.java, { onPlayerCreate(it) }, true, EventPriority.MONITOR)
    }

    @EventHandler
    fun onPlayerCreate(e: SynapsePlayerCreationEvent) {
        if(GTAnticheat.GT_MODE) {
            if(e.baseClass == SynapsePlayer11::class.java) {
                e.playerClass = GTSynapseCheatPlayer11::class.java
            } else {
                e.playerClass = GTSynapseCheatPlayer::class.java
            }
        } else {
            e.playerClass = SynapseCheatPlayer::class.java
        }
    }
}
