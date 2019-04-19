package cz.creeperface.nukkit.gac.punishment

import cn.nukkit.Player
import cz.creeperface.nukkit.gac.GTAnticheat
import cz.creeperface.nukkit.gac.utils.Messages

/**
 * @author CreeperFace
 */
class DefaultPunishmentManager(private val plugin: GTAnticheat) : PunishmentManager {

    override fun doBan(p: Player, reason: String) {

    }

    override fun doKick(p: Player, reason: String) {
        p.kick(Messages.translate("kick_player", reason, p.displayName), false)

        plugin.server.broadcastMessage(Messages.translate("kick_broadcast", reason, p.displayName))
    }
}