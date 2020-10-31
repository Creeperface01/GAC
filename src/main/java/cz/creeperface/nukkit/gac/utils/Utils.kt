@file:Suppress("ConstantConditionIf")

package cz.creeperface.nukkit.gac.utils

import cn.nukkit.Player
import cn.nukkit.Server
import cn.nukkit.math.Vector2
import cn.nukkit.math.Vector3
import cz.creeperface.nukkit.gac.GTAnticheat
import cz.creeperface.nukkit.gac.player.NukkitCheatPlayer
import cz.creeperface.nukkit.gac.player.SynapseCheatPlayer
import kotlin.reflect.KClass

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

fun Vector3.toVec2() = Vector2(x, z)

val gacPlayerClass: KClass<out Player> by lazy {
    if (Server.getInstance().pluginManager.getPlugin("SynapseAPI") != null) {
        SynapseCheatPlayer::class
    } else {
        NukkitCheatPlayer::class as KClass<out Player>
    }
}