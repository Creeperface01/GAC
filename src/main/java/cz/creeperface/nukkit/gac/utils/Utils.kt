@file:Suppress("ConstantConditionIf")

package cz.creeperface.nukkit.gac.utils

import cn.nukkit.Player
import cz.creeperface.nukkit.gac.GTAnticheat

inline fun debug(message: () -> Any?) {
    if (GTAnticheat.DEBUG) {
        GTAnticheat.instance.logger.info(message().toString())
    }
}

@Suppress("NOTHING_TO_INLINE")
inline fun Player.checkGamemode(): Boolean {
    return if (GTAnticheat.DEBUG_CREATIVE) {
        true
    } else {
        this.gamemode == 0
    }
}