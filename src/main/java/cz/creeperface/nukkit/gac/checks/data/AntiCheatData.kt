package cz.creeperface.nukkit.gac.checks.data

import cn.nukkit.level.Location
import cn.nukkit.level.Position
import cn.nukkit.math.Vector3

/**
 * Created by CreeperFace on 1. 12. 2016.
 */
class AntiCheatData {

    lateinit var lastPos: Location


    var lastOnGround: Long = 0
    
    var freeriding: Long = 0


    lateinit var lastGroundPos: Location


    var isLastPacketOnGround = true


    var isJumping = false


    var lastJump: Long = 0

    var lastDownMove: Long = 0

    var lastPacketJump: Long = 0


    lateinit var lastJumpPos: Location


    var lastCheck: Long = 0


    var isOnGround = true


    var motionY = 0.0


    var lastTeleport: Long = 0


    var isTeleport = false

    var teleportPosition = Position()


    var lastHit: Long = 0


    var lastLiquid = Vector3()


    var lastSlab = Vector3()


    var lastSlabTime = java.lang.Long.MAX_VALUE

    var wasInWater = false


    var isInLiquid = false

    var lastBlockBreak: Long = 0

    //public Vector3 waterDirection = new Vector3();

    /**
     * if player enter into a liquid (water)
     */
    var enter: Long = 0

    val motionData = MotionData()

    var horizontalFlightPoints = 0

    var lastHighestPos: Long = 0

    var largeGroundStateCheck = false

    var nukerPoints = 0
    var flyPoints = 0
    var glidePoints = 0
    var speedPoints = 0
    var speedminePoints = 0
    var inAirPoints = 0
    var killAuraPoints = 0

    /**
     * checks data
     */

    var packetsData = PacketsData()
}
