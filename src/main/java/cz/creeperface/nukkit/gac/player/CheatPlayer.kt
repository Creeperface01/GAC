package cz.creeperface.nukkit.gac.player

import cn.nukkit.Player
import cz.creeperface.nukkit.gac.ACData

class CheatPlayer(val playerInstance: Player, cheatPlayer: ICheatPlayer) : ICheatPlayer by cheatPlayer {

    override val acData: ACData = ACData(playerInstance)

//    override fun checkGroundState(large: Boolean) {
//        var onGround = false
//
//        val realBB = this.boundingBox.clone()
//
//        realBB.maxY = realBB.minY + 0.1
//        realBB.minY -= 0.4
//
//        if (large) {
//            realBB.expand(0.2, 0.0, 0.2)
//        }
//
//        this.blocksUnder = null
//
//        val bb = realBB.clone()
//        bb.minY -= 0.6
//        for (block in this.getBlocksUnder(bb)) {
//            if (!block.canPassThrough() && block.collidesWith(realBB)) {
//                onGround = true
//                break
//            }
//        }
//
//        this.onGround = onGround
//        this.isCollided = this.onGround
//    }
//
//    override fun handleMovePacket(packet: MovePlayerPacket) {
//        if (!connected) {
//            return
//        }
//
//        Timings.getReceiveDataPacketTiming(packet).use { timing ->
//            val ev = DataPacketReceiveEvent(this, packet)
//            this.server.pluginManager.callEvent(ev)
//            if (ev.isCancelled) {
//                timing.stopTiming()
//                return
//            }
//
//            if (this.teleportPosition != null) {
//                timing.stopTiming()
//                return
//            }
//
//            val newPos = Location(packet.x.toDouble(), (packet.y - this.eyeHeight).toDouble(), packet.z.toDouble(), packet.headYaw.toDouble(), packet.pitch.toDouble())
//            var revert = false
//
//            if (newPos.distanceSquared(this) < 0.0001 && (packet.yaw % 360).toDouble() == this.yaw && (packet.pitch % 360).toDouble() == this.pitch) {
//                timing.stopTiming()
//                return
//            }
//
//            if (!this.isAlive || !this.spawned) {
//                revert = true
//                this.forceMovement = Vector3(this.x, this.y, this.z)
//            }
//
//            if (this.forceMovement != null && (newPos.distanceSquared(this.forceMovement) > 0.2 || revert)) {
//                this.sendPosition(this.forceMovement, packet.yaw.toDouble(), packet.pitch.toDouble(), MovePlayerPacket.MODE_RESET)
//            } else {
//                packet.yaw %= 360f
//                packet.pitch %= 360f
//
//                if (packet.yaw < 0) {
//                    packet.yaw += 360f
//                }
//
//                this.setRotation(packet.yaw.toDouble(), packet.pitch.toDouble())
//                this.forceMovement = null
//                val packetEntry = this.packetsData[MovePlayerPacket.NETWORK_ID]!!
//
//                if (!processPacketCheck(packetEntry)) {
//                    //return;
//                }
//
//                val distanceSquared = newPos.distanceSquared(this)
//                if (this.chunk == null || !this.chunk.isGenerated) {
//                    val newPosV2 = this.level.getChunk(newPos.x.toInt() shr 4, newPos.z.toInt() shr 4, false)
//                    if (newPosV2 != null && newPosV2.isGenerated) {
//                        if (this.chunk != null) {
//                            this.chunk.removeEntity(this)
//                        }
//
//                        this.chunk = newPosV2
//                    } else {
//                        revert = true
//                        this.nextChunkOrderRun = 0
//                    }
//                }
//
//                val newPosV2 = Vector2(newPos.x, newPos.z)
//                val distance = newPosV2.distance(this.x, this.z)
//
//                if (!revert && distanceSquared != 0.0) {
//                    val dx = newPos.x - this.x
//                    val dy = newPos.y - this.y
//                    val dz = newPos.z - this.z
//
//                    this.newPosition = Vector3()
//                    this.fastMove(dx, dy, dz)
//                    if (this.newPosition == null) {
//                        return  //maybe solve that in better way
//                    }
//                    this.newPosition = null
//                    /*this.x = NukkitMath.round(this.x, 4);
//                    this.y = NukkitMath.round(this.y, 4);
//                    this.z = NukkitMath.round(this.z, 4);*/
//
//                    val diffX = this.x - newPos.x
//                    var diffY = this.y - newPos.y
//                    val diffZ = this.z - newPos.z
//
//                    val yS = 0.5 + this.ySize
//                    if (diffY >= -yS || diffY <= yS) {
//                        diffY = 0.0
//                    }
//
//                    if (diffX != 0.0 || diffY != 0.0 || diffZ != 0.0) {
//                        this.x = newPos.x
//                        this.y = newPos.y
//                        this.z = newPos.z
//                        val radius = (this.width / 2).toDouble()
//                        this.boundingBox.setBounds(this.x - radius, this.y, this.z - radius, this.x + radius, this.y + this.height, this.z + radius)
//                    }
//                }
//
//                val from = Location(this.lastX, this.lastY, this.lastZ, this.lastYaw, this.lastPitch, this.level)
//                val to = this.location
//                //double delta = Math.pow(this.lastX - to.x, 2) + Math.pow(this.lastY - to.y, 2) + Math.pow(this.z - to.z, 2);
//                //double deltaAngle = Math.abs(this.lastYaw - to.yaw) + Math.abs(this.lastPitch - to.pitch);
//
//                if (revert) {
//                    if (distanceSquared == 0.0) {
//                        this.speed = Vector3(0.0, 0.0, 0.0)
//                    }
//                } else {
//                    val firstMove = this.firstMove
//
//                    this.firstMove = false
//                    this.lastX = to.x
//                    this.lastY = to.y
//                    this.lastZ = to.z
//                    this.lastYaw = to.yaw
//                    this.lastPitch = to.pitch
//
//                    if (!firstMove) {
//                        val blocksAround = if (this.blocksAround != null) ArrayList(this.blocksAround) else null
//                        val collidingBlocks = if (this.collisionBlocks != null) ArrayList(this.collisionBlocks) else null
//
//                        val moveEvent = PlayerMoveEvent(this, from, to)
//
//                        this.blocksAround = null
//                        this.collisionBlocks = null
//                        this.blocksUnder = null
//
//                        this.server.pluginManager.callEvent(moveEvent)
//
//                        revert = moveEvent.isCancelled
//                        if (!revert) {
//                            if (to != moveEvent.to) {
//                                this.teleport(moveEvent.to, null)
//                            } else {
//                                this.addMovement(this.x, this.y + this.eyeHeight.toDouble(), this.z, this.yaw, this.pitch, this.yaw)
//                            }
//
//                            this.updateFallState(this.onGround)
//                            if (this.onGround || this.y > this.highestPosition) {
//                                this.highestPosition = this.y
//                            }
//                        } else {
//                            this.blocksAround = blocksAround
//                            this.collisionBlocks = collidingBlocks
//                        }
//                    }
//
//                    if (!this.isSpectator) {
//                        this.checkNearEntities()
//                    }
//
//                    this.speed = from.subtract(to)
//                }
//
//                if (!revert && this.isFoodEnabled) {
//                    this.hungerUpdate(distance)
//                }
//
//                if (revert) {
//                    this.lastX = from.x
//                    this.lastY = from.y
//                    this.lastZ = from.z
//
//                    this.lastYaw = from.yaw
//                    this.lastPitch = from.pitch
//
//                    this.x = from.x
//                    this.y = from.y
//                    this.z = from.z
//                    this.yaw = from.yaw
//                    this.pitch = from.pitch
//
//                    this.sendPosition(from, from.yaw, from.pitch, MovePlayerPacket.MODE_TELEPORT)
//                    this.forceMovement = Vector3(from.x, from.y, from.z)
//                } else {
//                    this.forceMovement = null
//                    if (distanceSquared != 0.0 && this.nextChunkOrderRun > 20) {
//                        this.nextChunkOrderRun = 20
//                    }
//                }
//
//                if (this.riding != null && this.riding is EntityBoat) {
//                    this.riding.setPositionAndRotation(this.temporalVector.setComponents(packet.x.toDouble(), (packet.y - 1.0f).toDouble(), packet.z.toDouble()), ((packet.headYaw + 90.0f) % 360.0f).toDouble(), 0.0)
//                }
//            }
//        }
//    }
}