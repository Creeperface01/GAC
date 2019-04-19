package cz.creeperface.nukkit.gac.punishment

import cn.nukkit.Player

/**
 * @author CreeperFace
 */
interface PunishmentManager {

    fun doKick(p: Player, reason: String)

    fun doBan(p: Player, reason: String)
}