package cz.creeperface.nukkit.gac.player

import cn.nukkit.IPlayer
import cn.nukkit.block.Block
import cn.nukkit.math.AxisAlignedBB
import cn.nukkit.math.Vector3
import cn.nukkit.network.protocol.MovePlayerPacket
import cz.creeperface.nukkit.gac.ACData
import cz.creeperface.nukkit.gac.player.utils.ILocation

/**
 * @author CreeperFace
 */
interface ICheatPlayer /*: ILocation, IPlayer*/ {

    val acData: ACData

//    val boundingBox: AxisAlignedBB

    var blocksUnder: MutableList<Block>?

//    var onGround: Boolean

//    var isCollided: Boolean

//    val connected: Boolean

//    var teleportPosition: Vector3

    fun checkGroundState(large: Boolean = false)

    fun getBlocksUnder(bb: AxisAlignedBB?): List<Block>

    fun handleMovePacket(packet: MovePlayerPacket)
}
