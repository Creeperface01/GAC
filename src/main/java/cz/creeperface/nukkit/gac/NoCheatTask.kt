package cz.creeperface.nukkit.gac

import cn.nukkit.AdventureSettings
import cn.nukkit.Player
import cn.nukkit.Server
import cn.nukkit.block.BlockLiquid
import cn.nukkit.entity.Entity
import cn.nukkit.item.Item
import cn.nukkit.math.AxisAlignedBB
import cn.nukkit.math.NukkitMath
import cn.nukkit.math.Vector3
import cn.nukkit.scheduler.Task
import cz.creeperface.nukkit.gac.checks.data.AntiCheatData
import cz.creeperface.nukkit.gac.player.ICheatPlayer
import cz.creeperface.nukkit.gac.utils.*

class NoCheatTask(
        /*private Vector3 temporalVector = new Vector3();
    private Vector3 temporalVector1 = new Vector3();*/

        private val plugin: GTAnticheat) : Task() {

    override fun onRun(currentTick: Int) = GACTimings.cheatTask.use {
        val time = System.currentTimeMillis()

        for (p in Server.getInstance().onlinePlayers.values) {
            if (p !is ICheatPlayer) {
                continue
            }

            val acData = p.acData

            if (GTAnticheat.conf.kick) {
                val reason = plugin.kickQueue.remove(p.id)
                if (reason != null) {
                    plugin.punishmentManager.doKick(p, reason)
//                    p.kick(Messages.translate("kick_player", reason, p.displayName), false)
//
//                    plugin.server.broadcastMessage(Messages.translate("kick_broadcast", reason, p.displayName))
                    continue
                }

                val banReason = plugin.banQueue.remove(p.id)

                if (banReason != null) {
                    //p.kick(Lang.translate("kick", data, banReason), false);
                    p.kick(Messages.translate("kick_player", banReason, p.displayName), false)
                    continue
                }
            }

//            if (data.isInLobby()) {
//                //System.out.println("lobby");
//                continue
//            }

            if (!p.isSurvival || !p.isAlive || p.ticksLived < 40 || p.riding != null) {
                //System.out.println("survvial");
                continue
            }

            var points = 0

            val cheatData = acData.antiCheatData

            if (!p.checkGamemode() || p.adventureSettings.get(AdventureSettings.Type.FLYING) || cheatData.lastPos.getLevel().id != p.getLevel().id || cheatData.isTeleport) {
                //System.out.println("task check 1");
                val current = p.clone()

//                debug { "Update ground pos (task)" }
                cheatData.lastGroundPos = current
                cheatData.lastOnGround = time
                cheatData.lastJumpPos = current
                cheatData.lastJump = time
                cheatData.motionData.clear()
                acData.speedData.lastNonSpeedPos = current
                acData.speedData.lastNonBhopPos = current
                points = -1


            } else if (shouldCheck(p, CheckType.FLY)) {
                val motionData = acData.motionData

                if (motionData.isEmpty) {
                    //double maxDistance = getMaxDistance(p, data, time - acData.speedData.lastCheck);

                    if (time - cheatData.lastHit <= 1700) {
                        continue
                    }

                    val prev = cheatData.lastPos
                    //System.out.println("task check 2");

                    if (p.y > -20 && !p.onGround && !cheatData.isOnGround && cheatData.lastGroundPos.distance(p) > 0.3 && prev.y == p.y) {
                        val inAirTime = (time - cheatData.lastOnGround).toInt()

                        if (inAirTime > 1000) {
                            if (!acData.antiCheatData.largeGroundStateCheck) {
                                (p as ICheatPlayer).checkGroundState(true)
                            }

                            if (!p.isOnGround) {
                                p.motion = p.temporalVector.setComponents(0.0, -500.0, 0.0)
                                points = 1
                                cheatData.flyPoints++

                                if (cheatData.flyPoints > 5) {
                                    p.teleport(cheatData.lastGroundPos)
                                    cheatData.flyPoints = 0
                                }
                            }
                        }
                        //System.out.println("task check !onground");
                    }

                    /*double actualDistance = temporalVector.setComponents(prev.x, 0, prev.z).distance(temporalVector1.setComponents(p.x, 0, p.z));
                    double diff = maxDistance - actualDistance;

                    //System.out.println("distance diff: "+diff);
                    if (diff < 0) {
                        //System.out.println("p onGround");
                        points = -1;
                        if (cheatData.speedPoints > 0) {
                            cheatData.speedPoints = Math.max(0, cheatData.speedPoints - 7);
                        }
                    }*/
                    //}

                    if (cheatData.speedPoints > 0) {
                        cheatData.speedPoints = 0.coerceAtLeast(cheatData.speedPoints - 2)
                    }
                } else {
                    //System.out.println("motion data");
                    val timeY = motionData.timeY

                    if ((p.isOnGround || cheatData.isOnGround) && time > timeY && time > motionData.time || motionData.groundTime != (-1).toLong() && time - motionData.groundTime > 1500) {
                        acData.motionData.clear()
                        acData.speedData.lastNonSpeedPos = p.clone()
                        acData.speedData.lastNonBhopPos = acData.speedData.lastNonSpeedPos
                    } else if (!p.isOnGround && !cheatData.isOnGround && time - timeY > 3000 && time > motionData.time) {
                        p.motion = p.temporalVector.setComponents(0.0, -500.0, 0.0)
                    }
                }
            }

            cheatData.wasInWater = cheatData.isInLiquid

            plugin.doKickCheck(acData, p)

            cheatData.lastPos = p.location.clone()
            acData.speedData.lastCheck = time
        }
    }

    private fun getMaxDistance(p: Player, acData: cz.creeperface.nukkit.gac.ACData, diff: Long): Double {
        var speed = if (acData.speedData.lastSpeedChange + diff > System.currentTimeMillis()) SPRINTING_SPEED else if (p.isSprinting) SPRINTING_SPEED else if (p.isSneaking) SNEAKING_SPEED else WALKING_SPEED

        if (System.currentTimeMillis() - acData.antiCheatData.lastJump < 2500) {
            speed += 1.5
        }

        if (acData.antiCheatData.isInLiquid && acData.antiCheatData.wasInWater) {
            speed = if (p.isSneaking) WATER_SNEAK_SPEED else WATER_SPEED
        }

        speed *= (p.movementSpeed * 10).toDouble()

        return speed * (diff.toDouble() / 900.0)
    }

    companion object {

        private val WALKING_SPEED = 5.0 //4.317
        private val SPRINTING_SPEED = 6.5 //5.612
        private val SNEAKING_SPEED = 1.7 //1.31
        private val WATER_SPEED = 2.6
        private val WATER_SNEAK_SPEED = 0.9

        fun isInAir(p: Player, data: AntiCheatData): Boolean {
            for (b in p.getCollisionBlocks()) {

                if (b is BlockLiquid) {
                    data.lastLiquid = p.clone()
                    data.lastOnGround = System.currentTimeMillis()
                    data.lastGroundPos = p.clone()
//                    debug { "set ground pos (liquid)" }
                    data.isOnGround = true
                    p.highestPosition = p.y
                    return false
                }

                if (b.id == Item.LADDER || b.id == Item.VINE) {
//                    debug { "set ground pos (climbable)" }
                    data.lastOnGround = System.currentTimeMillis()
                    data.lastGroundPos = p.clone()
                    data.isOnGround = true
                    p.highestPosition = p.y
                    return false
                }
            }

            return true
        }

//        fun getBlocksUnder(p: Player): Array<Block> {
//            val bb = p.getBoundingBox().clone()
//            bb.maxY = bb.minY + 0.5
//            bb.minY -= 1
//
//            val minX = NukkitMath.floorDouble(bb.minX)
//            val minY = NukkitMath.floorDouble(bb.minY)
//            val minZ = NukkitMath.floorDouble(bb.minZ)
//            val maxX = NukkitMath.ceilDouble(bb.maxX)
//            val maxY = NukkitMath.ceilDouble(bb.maxY)
//            val maxZ = NukkitMath.ceilDouble(bb.maxZ)
//
//            for (z in minZ..maxZ) {
//                for (x in minX..maxX) {
//                    for (y in minY..maxY) {
//                        val block = p.getLevel().getBlock(p.temporalVector.setComponents(x.toDouble(), y.toDouble(), z.toDouble()))
//                        if (block.id != 0 && block.collidesWithBB(bb) && (!block.canPassThrough() || block.id == Item.LADDER || block.id == Item.VINE || block is BlockLiquid)) {
//                            return arrayOf(block)
//                        }
//                    }
//                }
//            }
//
//            return arrayOf()
//        }

        @JvmOverloads
        fun rangeCheck(bb: AxisAlignedBB, damager: Entity, maxDistance: Double = 5.0): Boolean {
            val from = Vector3(damager.x, damager.y + damager.eyeHeight, damager.z)
            if (bb.isVectorInside(from)) {
                return true
            }

            val minX = bb.minX
            val minY = bb.minY
            val minZ = bb.minZ

            val maxX = bb.maxX
            val maxY = bb.maxY
            val maxZ = bb.maxZ

            val finalVector = Vector3()

            finalVector.x = NukkitMath.clamp(from.x, minX, maxX)
            finalVector.y = NukkitMath.clamp(from.y, minY, maxY)
            finalVector.z = NukkitMath.clamp(from.z, minZ, maxZ)

            return finalVector.distance(from) < maxDistance
        }

        fun checkGroundState(p: Player) {

        }
    }
}
