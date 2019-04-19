package cz.creeperface.nukkit.gac.checks.data

import cn.nukkit.level.Location

/**
 * Created by CreeperFace on 20. 11. 2016.
 */
class PacketsData {

    var lastUpdate: Long = 0
    var lastTick: Long = 0
    var count = 0
    var revertNumber = 0

    var lastLagTick: Long = 0
    var lastLagDuration: Long = 0

    var lastPos = Location()
}
