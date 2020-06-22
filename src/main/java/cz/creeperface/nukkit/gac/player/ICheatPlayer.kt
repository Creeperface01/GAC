package cz.creeperface.nukkit.gac.player

import cn.nukkit.IPlayer
import cn.nukkit.Player
import cn.nukkit.block.Block
import cn.nukkit.level.Location
import cn.nukkit.math.AxisAlignedBB
import cn.nukkit.math.Vector3
import cz.creeperface.nukkit.gac.ACData

/**
 * @author CreeperFace
 */
interface ICheatPlayer : IPlayer {

    val acData: ACData

    var nextChunkOrderRun: Int

    var forceMovement: Vector3?

    var newPosition: Vector3?

    var teleportPosition: Vector3?

    val ySize: Float

    var startAirTicks: Int

    val currentPos: Location

    override fun getPlayer(): Player

    fun checkNearEntities()

    fun updateFallState(onGround: Boolean)

    fun setMotion(motion: Vector3): Boolean

    fun getDrag(): Float

    fun getGravity(): Float

    fun getBlocksUnder(boundingBox: AxisAlignedBB?): List<Block>

    fun checkGroundState(large: Boolean)

    fun jump()
}
