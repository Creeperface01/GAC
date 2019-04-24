package cz.creeperface.nukkit.gac.player

import cn.nukkit.block.Block
import cn.nukkit.entity.item.EntityBoat
import cn.nukkit.event.entity.EntityMotionEvent
import cn.nukkit.event.player.PlayerMoveEvent
import cn.nukkit.event.server.DataPacketReceiveEvent
import cn.nukkit.level.Location
import cn.nukkit.math.AxisAlignedBB
import cn.nukkit.math.NukkitMath
import cn.nukkit.math.Vector2
import cn.nukkit.math.Vector3
import cn.nukkit.network.SourceInterface
import cn.nukkit.network.protocol.*
import co.aikar.timings.Timings
import cz.creeperface.nukkit.gac.GTAnticheat
import cz.creeperface.nukkit.gac.checks.PacketCountCheck
import cz.creeperface.nukkit.gac.checks.collidesWith
import org.itxtech.synapseapi.SynapseEntry
import org.itxtech.synapseapi.SynapsePlayer
import java.util.*

/**
 * Created by CreeperFace on 25.7.2017.
 */
class SynapseCheatPlayer(interfaz: SourceInterface, entry: SynapseEntry, clientID: Long?, ip: String, port: Int) : SynapsePlayer(interfaz, entry, clientID, ip, port), ICheatPlayer {

    override var blocksUnder: MutableList<Block>? = ArrayList()

    var fakePlayerHits = 0

    override val acData = cz.creeperface.nukkit.gac.ACData(this)

    private val packetsData = object : HashMap<Byte, PacketCountCheck.Entry>() {
        init {
            put(InteractPacket.NETWORK_ID, PacketCountCheck().Entry())
            put(MovePlayerPacket.NETWORK_ID, PacketCountCheck().Entry())
        }
    }

    override fun handleDataPacket(packet: DataPacket) {
        if (packet.pid() == MovePlayerPacket.NETWORK_ID) {
            this.handleMovePacket(packet as MovePlayerPacket)
            return
        }

        /*if(packet.pid() == InteractPacket.NETWORK_ID) {
            InteractPacket pk = (InteractPacket) packet;

            if(pk.action == InteractPacket.ACTION_LEFT_CLICK) {
                if(pk.target == FakePlayer.ID) { //fake player
                    MTCore.getInstance().getLogger().warning(this.getName()+" has hit fake player");
                    fakePlayerHits++;

                    if(fakePlayerHits > 20) {
                        this.getPlayerData().acData.antiCheatData.killAuraPoints++;
                        gac.getInstance().banQueue.put(this.getId(), "kill aura");
                    }

                    return;
                }
            }
        }*/

        if (packet.pid() == ProtocolInfo.PLAYER_ACTION_PACKET) {
            if ((packet as PlayerActionPacket).action == PlayerActionPacket.ACTION_JUMP) {
                val time = System.currentTimeMillis()

                acData.antiCheatData.lastPacketJump = time
                //System.out.println("jump 0");

                if (acData.antiCheatData.lastGroundPos.distance(this) < 0.4) {
                    GTAnticheat.instance.onJump(this, acData)
                    //System.out.println("jump");
                }
                //}

                val motionData = acData.motionData
                if (!motionData.isEmpty && !motionData.ground) {
                    val timeY = motionData.timeY

                    if (time > timeY && time > motionData.time && motionData.groundTime != (-1).toLong() && time - motionData.groundTime > 1500) {
                        //System.out.println("jump ground");
                        motionData.ground = true
                    }
                }
            }
        }

        super.handleDataPacket(packet)
    }

    /*private int revertCount = 0;
    private int lastCheckTick = 0;
    private long lastCheckTime = 0;
    private int packetCount = 0;*/

    override fun handleMovePacket(packet: MovePlayerPacket) {
        if (!connected) {
            return
        }

        Timings.getReceiveDataPacketTiming(packet).use { timing ->
            val ev = DataPacketReceiveEvent(this, packet)
            this.server.pluginManager.callEvent(ev)
            if (ev.isCancelled) {
                timing.stopTiming()
                return
            }

            if (this.teleportPosition != null) {
                timing.stopTiming()
                return
            }

            val newPos = Location(packet.x.toDouble(), (packet.y - this.eyeHeight).toDouble(), packet.z.toDouble(), packet.headYaw.toDouble(), packet.pitch.toDouble())
            var revert = false

            if (newPos.distanceSquared(this) < 0.0001 && (packet.yaw % 360).toDouble() == this.yaw && (packet.pitch % 360).toDouble() == this.pitch) {
                timing.stopTiming()
                return
            }

            if (!this.isAlive || !this.spawned) {
                revert = true
                this.forceMovement = Vector3(this.x, this.y, this.z)
            }

            if (this.forceMovement != null && (newPos.distanceSquared(this.forceMovement) > 0.2 || revert)) {
                this.sendPosition(this.forceMovement, packet.yaw.toDouble(), packet.pitch.toDouble(), MovePlayerPacket.MODE_RESET)
            } else {
                packet.yaw %= 360f
                packet.pitch %= 360f

                if (packet.yaw < 0) {
                    packet.yaw += 360f
                }

                this.setRotation(packet.yaw.toDouble(), packet.pitch.toDouble())
                this.forceMovement = null
                val packetEntry = this.packetsData[MovePlayerPacket.NETWORK_ID]!!

                if (!processPacketCheck(packetEntry)) {
                    //return;
                }

                val distanceSquared = newPos.distanceSquared(this)
                if (this.chunk == null || !this.chunk.isGenerated) {
                    val newPosV2 = this.level.getChunk(newPos.x.toInt() shr 4, newPos.z.toInt() shr 4, false)
                    if (newPosV2 != null && newPosV2.isGenerated) {
                        if (this.chunk != null) {
                            this.chunk.removeEntity(this)
                        }

                        this.chunk = newPosV2
                    } else {
                        revert = true
                        this.nextChunkOrderRun = 0
                    }
                }

                val newPosV2 = Vector2(newPos.x, newPos.z)
                val distance = newPosV2.distance(this.x, this.z)

                if (!revert && distanceSquared != 0.0) {
                    val dx = newPos.x - this.x
                    val dy = newPos.y - this.y
                    val dz = newPos.z - this.z

                    this.newPosition = Vector3()
                    this.fastMove(dx, dy, dz)
                    if (this.newPosition == null) {
                        return  //maybe solve that in better way
                    }
                    this.newPosition = null
                    /*this.x = NukkitMath.round(this.x, 4);
                    this.y = NukkitMath.round(this.y, 4);
                    this.z = NukkitMath.round(this.z, 4);*/

                    val diffX = this.x - newPos.x
                    var diffY = this.y - newPos.y
                    val diffZ = this.z - newPos.z

                    val yS = 0.5 + this.ySize
                    if (diffY >= -yS || diffY <= yS) {
                        diffY = 0.0
                    }

                    if (diffX != 0.0 || diffY != 0.0 || diffZ != 0.0) {
                        this.x = newPos.x
                        this.y = newPos.y
                        this.z = newPos.z
                        val radius = (this.width / 2).toDouble()
                        this.boundingBox.setBounds(this.x - radius, this.y, this.z - radius, this.x + radius, this.y + this.height, this.z + radius)
                    }
                }

                val from = Location(this.lastX, this.lastY, this.lastZ, this.lastYaw, this.lastPitch, this.level)
                val to = this.location
                //double delta = Math.pow(this.lastX - to.x, 2) + Math.pow(this.lastY - to.y, 2) + Math.pow(this.z - to.z, 2);
                //double deltaAngle = Math.abs(this.lastYaw - to.yaw) + Math.abs(this.lastPitch - to.pitch);

                if (revert) {
                    if (distanceSquared == 0.0) {
                        this.speed = Vector3(0.0, 0.0, 0.0)
                    }
                } else {
                    val firstMove = this.firstMove

                    this.firstMove = false
                    this.lastX = to.x
                    this.lastY = to.y
                    this.lastZ = to.z
                    this.lastYaw = to.yaw
                    this.lastPitch = to.pitch

                    if (!firstMove) {
                        val blocksAround = if (this.blocksAround != null) ArrayList(this.blocksAround) else null
                        val collidingBlocks = if (this.collisionBlocks != null) ArrayList(this.collisionBlocks) else null

                        val moveEvent = PlayerMoveEvent(this, from, to)

                        this.blocksAround = null
                        this.collisionBlocks = null
                        this.blocksUnder = null

                        this.server.pluginManager.callEvent(moveEvent)

                        revert = moveEvent.isCancelled
                        if (!revert) {
                            if (to != moveEvent.to) {
                                this.teleport(moveEvent.to, null)
                            } else {
                                this.addMovement(this.x, this.y + this.eyeHeight.toDouble(), this.z, this.yaw, this.pitch, this.yaw)
                            }

                            this.updateFallState(this.onGround)
                            if (this.onGround || this.y > this.highestPosition) {
                                this.highestPosition = this.y
                            }
                        } else {
                            this.blocksAround = blocksAround
                            this.collisionBlocks = collidingBlocks
                        }
                    }

                    if (!this.isSpectator) {
                        this.checkNearEntities()
                    }

                    this.speed = from.subtract(to)
                }

                if (!revert && this.isFoodEnabled) {
                    this.hungerUpdate(distance)
                }

                if (revert) {
                    this.lastX = from.x
                    this.lastY = from.y
                    this.lastZ = from.z

                    this.lastYaw = from.yaw
                    this.lastPitch = from.pitch

                    this.x = from.x
                    this.y = from.y
                    this.z = from.z
                    this.yaw = from.yaw
                    this.pitch = from.pitch

                    this.sendPosition(from, from.yaw, from.pitch, MovePlayerPacket.MODE_TELEPORT)
                    this.forceMovement = Vector3(from.x, from.y, from.z)
                } else {
                    this.forceMovement = null
                    if (distanceSquared != 0.0 && this.nextChunkOrderRun > 20) {
                        this.nextChunkOrderRun = 20
                    }
                }

                if (this.riding != null && this.riding is EntityBoat) {
                    this.riding.setPositionAndRotation(this.temporalVector.setComponents(packet.x.toDouble(), (packet.y - 1.0f).toDouble(), packet.z.toDouble()), ((packet.headYaw + 90.0f) % 360.0f).toDouble(), 0.0)
                }
            }
        }
    }

    private fun hungerUpdate(distance: Double) {
        var distance = distance

        if ((this.isSurvival || this.isAdventure) && distance >= 0.05) {
            var jump = 0.0
            val swimming = if (this.isInsideOfWater) 0.015 * distance else 0.0
            if (swimming != 0.0) {
                distance = 0.0
            }

            if (this.isSprinting) {
                if (this.inAirTicks == 3 && swimming == 0.0) {
                    jump = 0.7
                }

                this.foodData.updateFoodExpLevel(0.1 * distance + jump + swimming)
            } else {
                if (this.inAirTicks == 3 && swimming == 0.0) {
                    jump = 0.2
                }

                this.foodData.updateFoodExpLevel(0.01 * distance + jump + swimming)
            }
        }
    }

    //public List<Block> blocksUnder = null;

    override fun checkGroundState(movX: Double, movY: Double, movZ: Double, dx: Double, dy: Double, dz: Double) {
        //this.checkGroundState(movX, movY, movZ, dx, dy, dz, false);
    }

    override fun checkGroundState(large: Boolean) {
        var onGround = false

        val realBB = this.boundingBox.clone()

        realBB.maxY = realBB.minY + 0.1
        realBB.minY -= 0.4

        if (large) {
            realBB.expand(0.2, 0.0, 0.2)
        }

        this.blocksUnder = null

        val bb = realBB.clone()
        bb.minY -= 0.6
        for (block in this.getBlocksUnder(bb)) {
            if (!block.canPassThrough() && block.collidesWith(realBB)) {
                onGround = true
                break
            }
        }

        this.onGround = onGround
        this.isCollided = this.onGround
    }

    override fun getBlocksUnder(bb: AxisAlignedBB?): List<Block> {
        var bb = bb
        if (blocksUnder == null || blocksUnder!!.isEmpty()) {
            blocksUnder = ArrayList()

            if (bb == null) {
                bb = this.boundingBox.clone()
                bb.maxY = bb.minY + 0.1
                bb.minY -= 0.4
            }

            val minX = NukkitMath.floorDouble(bb.minX)
            val minY = NukkitMath.floorDouble(bb.minY)
            val minZ = NukkitMath.floorDouble(bb.minZ)
            val maxX = NukkitMath.ceilDouble(bb.maxX)
            val maxY = NukkitMath.ceilDouble(bb.maxY)
            val maxZ = NukkitMath.ceilDouble(bb.maxZ)

            for (z in minZ..maxZ) {
                for (x in minX..maxX) {
                    for (y in minY..maxY) {
                        blocksUnder!!.add(this.level.getBlock(this.temporalVector.setComponents(x.toDouble(), y.toDouble(), z.toDouble())))
                    }
                }
            }
        }

        return ArrayList(blocksUnder!!)
    }

    override fun setMotion(motion: Vector3): Boolean {
        if (!this.justCreated) {
            val ev = EntityMotionEvent(this, motion)
            this.server.pluginManager.callEvent(ev)
            if (ev.isCancelled) {
                return false
            }
        }

        //this.motionX = motion.x
        //this.motionY = motion.y
        //this.motionZ = motion.z

        if (this.chunk != null) {
            //this.getLevel().addEntityMotion(this.chunk.getX(), this.chunk.getZ(), this.getId(), this.motionX, this.motionY, this.motionZ);
            val pk = SetEntityMotionPacket()
            pk.eid = this.id
            pk.motionX = motion.x.toFloat()
            pk.motionY = motion.y.toFloat()
            pk.motionZ = motion.z.toFloat()
            this.dataPacket(pk)
        }

        if (this.motionY > 0.0) {
            this.startAirTicks = (-Math.log(this.gravity.toDouble() / (this.gravity.toDouble() + this.drag.toDouble() * this.motionY)) / this.drag.toDouble() * 2.0 + 5.0).toInt()
        }

        return true
    }

    private fun processPacketCheck(packetEntry: PacketCountCheck.Entry): Boolean {
        ++packetEntry.packetCount
        val tick = this.getServer().tick
        if (tick - packetEntry.lastCheckTick >= 20) {
            val time = System.currentTimeMillis()

            val maxPackets = (time - packetEntry.lastCheckTime) / 50
            //System.out.println("packets: "+packetEntry.packetCount+"     max: "+maxPackets);
            if (packetEntry.packetCount > maxPackets * 2) {
                packetEntry.revertCount += 3
            } else if (packetEntry.packetCount.toDouble() > maxPackets.toDouble() * 1.5) {
                packetEntry.revertCount += 2
            } else if (packetEntry.packetCount.toDouble() > maxPackets.toDouble() * 1.25) {
                ++packetEntry.revertCount
            } else {
                packetEntry.revertCount = 0
            }

            packetEntry.packetCount = 0
            packetEntry.lastCheckTime = time
            packetEntry.lastCheckTick = tick
            if (packetEntry.revertCount > 6) {
                GTAnticheat.instance.kickQueue[this.getId()] = "Illegal speed"
                return false
            }
        }

        return true
    }
}
