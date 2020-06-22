package cz.creeperface.nukkit.gac.checks.data

import cn.nukkit.level.Location
import cn.nukkit.math.Vector3

/**
 * Created by CreeperFace on 1. 12. 2016.
 */
class SpeedData {

    var lastRevert: Long = 0
    var wasRevert = false

    lateinit var lastNonSpeedPos: Location
    lateinit var lastNonBhopPos: Location

    var lastSpeedType = SpeedType.WALK
    var currentSpeedType = SpeedType.WALK

    var lastSpeedChange: Long = 0

    var lastCheck: Long = 0

    var successCount = 0
    var smoothRevertCount = 0

    var lastMotion = Vector3()

    enum class SpeedType(val speed: Double) {
        SNEAK(0.19600677490234375),
        WALK(0.toDouble()),
        SPRINT(0.toDouble()),
        SWIM(0.19600677490234375)
    }

    companion object {

        const val friction = 1.3323920282449055070823188907453

        /**
         * move speed
         */

        //vertical
        const val WALK_SPEED = 0
        const val SPRINT_SPEED = 0
        const val SNEAK_SPEED = 0
        const val SWIM_SPEED = 0

        const val WATER_MODIFIER = 0

        //horizontal
        const val LADDER = 0.2
        const val JUMP = 0.55
        const val STAIRS = 0.5
        const val WATER = 0.0
        const val LAVA = 0.43 //0.4000000
    }
}
