package cz.creeperface.nukkit.gac.utils

import cn.nukkit.utils.TextFormat

/**
 * @author CreeperFace
 */
object Messages {

    private lateinit var messages: Map<String, String>

    fun load(map: Map<String, String>) {
        this.messages = map.mapValues { it.value.replace('&', TextFormat.ESCAPE) }.toMap()
    }

    fun translate(msg: String, reason: String, player: String = ""): String {
        return (messages[msg] ?: msg).replace("%reason%", reason).replace("%player%", player)
    }
}