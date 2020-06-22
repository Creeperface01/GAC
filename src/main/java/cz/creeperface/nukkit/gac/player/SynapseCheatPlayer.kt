package cz.creeperface.nukkit.gac.player

import cn.nukkit.level.Location
import cn.nukkit.math.AxisAlignedBB
import cn.nukkit.math.Vector3
import cn.nukkit.network.SourceInterface
import cn.nukkit.network.protocol.DataPacket
import org.itxtech.synapseapi.SynapseEntry
import org.itxtech.synapseapi.SynapsePlayer

/**
 * Created by CreeperFace on 25.7.2017.
 */
class SynapseCheatPlayer(interfaz: SourceInterface, entry: SynapseEntry, clientID: Long?, ip: String, port: Int) : SynapsePlayer(interfaz, entry, clientID, ip, port), ICheatPlayer {

    val cheatPlayer = CheatPlayer(this, this)

    override val acData = cheatPlayer.acData

    override var nextChunkOrderRun: Int
        get() = super.nextChunkOrderRun
        set(value) {
            super.nextChunkOrderRun = value
        }

    override var forceMovement: Vector3?
        get() = super.forceMovement
        set(value) {
            super.forceMovement = value
        }

    override var newPosition: Vector3?
        get() = super.newPosition
        set(value) {
            super.newPosition = value
        }

    override var teleportPosition: Vector3?
        get() = super.teleportPosition
        set(value) {
            super.teleportPosition = value
        }

    override val ySize: Float
        get() = super.ySize

    override var startAirTicks: Int
        get() = super.startAirTicks
        set(value) {
            super.startAirTicks = value
        }

    override val currentPos: Location
        get() = cheatPlayer.currentPos

    override fun getDrag(): Float {
        return super.getDrag()
    }

    override fun getGravity(): Float {
        return super.getGravity()
    }

    override fun checkNearEntities() {
        super.checkNearEntities()
    }

    override fun updateFallState(onGround: Boolean) {
        super.updateFallState(onGround)
    }

    override fun handleDataPacket(packet: DataPacket) {
        if (!this.cheatPlayer.handlePacket(packet)) {
            super.handleDataPacket(packet)
        }
    }

    override fun checkGroundState(movX: Double, movY: Double, movZ: Double, dx: Double, dy: Double, dz: Double) {

    }

    override fun setMotion(motion: Vector3) = this.cheatPlayer.setMotion(motion)

    override fun checkGroundState(large: Boolean) = this.cheatPlayer.checkGroundState(large)

    override fun getBlocksUnder(boundingBox: AxisAlignedBB?) = this.cheatPlayer.getBlocksUnder(boundingBox)

    override fun jump() = this.cheatPlayer.jump()
}
