package cz.creeperface.nukkit.gac.utils

import cz.creeperface.nukkit.gac.GTAnticheat

inline fun debug(message: () -> String) {
    if (GTAnticheat.DEBUG) {
        GTAnticheat.instance.logger.info(message())
    }
}