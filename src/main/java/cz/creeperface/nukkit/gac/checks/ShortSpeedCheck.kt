package cz.creeperface.nukkit.gac.checks

import cn.nukkit.AdventureSettings.Type
import cn.nukkit.event.player.PlayerMoveEvent
import cn.nukkit.math.Vector2
import cz.creeperface.nukkit.gac.GTAnticheat
import cz.creeperface.nukkit.gac.checks.data.SpeedData
import cz.creeperface.nukkit.gac.utils.*
import java.text.DecimalFormat

/**
 * Created by CreeperFace on 19. 11. 2016.
 */
object ShortSpeedCheck {

    //    var maxVert = 0.0
//    var maxInAir = 0.0

    private val format = DecimalFormat("#0.000000000000000")

    fun run(e: PlayerMoveEvent, acData: cz.creeperface.nukkit.gac.ACData): Boolean = GACTimings.speedCheck.use {
        val p = e.player

        val time = System.currentTimeMillis()

        if (p.adventureSettings.get(Type.FLYING) || p.riding != null || !acData.motionData.isEmpty || time - acData.antiCheatData.lastTeleport < 2000 || acData.antiCheatData.isTeleport) {
            return true
        }

        val from = e.from
        val to = e.to


        var inWater = false

        val cheatData = acData.antiCheatData
        if (cheatData.isInLiquid) {
            if (from.y == to.y && !p.onGround && p.distance(cheatData.lastGroundPos) > 0.3) {
                debug { "water revert" }
                e.setCancelled()
                return false
            }

            inWater = true
        }

        //int successCount = data.speedData.successCount;
        val motion = to.subtract(from)

        val speedData = acData.speedData
        if (from.x != to.x || from.z != to.z) {
            val fromv = Vector2(from.x, from.z)

            val distance = fromv.distance(to.x, to.z)
            var maxDistance = 0.23

            if (!p.onGround && shouldCheck(p, CheckType.BHOP)) { //bhop check
                val dist = motion.subtract(speedData.lastMotion).toVec2().lengthSquared()

//                if (maxInAir > 1) {
//                    maxInAir = 0.0
//                }
//
//                if (dist > maxInAir) {
//                    maxInAir = dist
//                }

//                debug {  "motion change: "+format.format(dist)+"   max: "+format.format(maxInAir) }

                val maxMotionChange = when (cheatData.sinceJump) {
                    0 -> 1.5
                    1 -> 0.06
                    else -> 0.002
                } * p.movementSpeed * 10

                if (dist > maxMotionChange && motion.toVec2().lengthSquared() > 0.004 && cheatData.lastGroundPos.distanceSquared(e.to) > 0.15) { //max in air motion diff
                    if (dist - maxMotionChange > maxMotionChange * 5) {
                        cheatData.bhopPoints += 2
                    } else {
                        cheatData.bhopPoints++
                    }

                    debug { "dist: " + format.format(dist) + "   max: " + format.format(maxMotionChange) + "  points: " + cheatData.bhopPoints + "  ground dist: " + cheatData.lastGroundPos.distanceSquared(e.to) }

                    if (cheatData.bhopPoints > 10) {
                        debug { "dist: " + dist + "  motion: " + motion.toVec2().lengthSquared() + "  ground dist: " + cheatData.lastGroundPos.distanceSquared(e.to) }
                        e.to = speedData.lastNonBhopPos
                        cheatData.bhopPoints = 0
                        return false
                    }
                } else {
                    if (cheatData.bhopPoints > 0) {
                        cheatData.bhopPoints--
                    } else {
                        acData.speedData.lastNonBhopPos = to
                    }
                }
            }

            if (p.adventureSettings.get(Type.FLYING)) {
                //System.out.println("fly ignore");
                return true
            } else if (p.isSprinting || acData.speedData.lastSpeedType == SpeedData.SpeedType.SPRINT && time - acData.speedData.lastSpeedChange < 800) {
                maxDistance = 0.3

                if (time - cheatData.lastJump < 1000) {
                    maxDistance += 0.06
                }
            } else if (p.isSneaking && acData.speedData.lastSpeedType > SpeedData.SpeedType.SNEAK && time - acData.speedData.lastSpeedChange < 800) {
                maxDistance = 0.131
            } else if (p.isSwimming || acData.speedData.lastSpeedType == SpeedData.SpeedType.SWIM && time - acData.speedData.lastSpeedChange < 800) {
                maxDistance = 0.1961
            }

            if (!p.isSwimming && inWater && time - cheatData.enter > 700) {
                maxDistance = 0.15

                if (p.isSprinting || acData.speedData.lastSpeedType == SpeedData.SpeedType.SPRINT && time - acData.speedData.lastSpeedChange < 800) {
                    maxDistance *= 1.32
                }
            }

            maxDistance *= 10 * p.movementSpeed
//            println("move speed: "+p.movementSpeed)

//            if(inWater)
//                MainLogger.getLogger().info("water_distance: $distance")

            //System.out.println(distance+"   :   "+maxDistance+"     Y: "+Math.abs(to.y - from.y));
//            if(p.isSwimming) {
//                MainLogger.getLogger().info("swim distance: $distance")
//            }

//            println("$distance x $maxDistance")

            if (distance > maxDistance && shouldCheck(p, CheckType.SPEED)) {
                cheatData.speedPoints++
                speedData.successCount = 0
                //System.out.println(distance+"   :   "+maxDistance+"     Y: "+Math.abs(to.y - from.y));

                val diff = distance - maxDistance

                if (diff > 0.5 || cheatData.speedPoints > 10) {
                    debug { p.isSprinting }
                    debug { acData.speedData.lastSpeedType }
                    debug { time - acData.speedData.lastSpeedChange }
                    debug { "horiz: $diff   current: $distance    max: $maxDistance" }
                    e.to = speedData.lastNonSpeedPos
                    cheatData.speedPoints = 0
                    return false
                }
            } else {
                speedData.successCount++

                if (speedData.successCount > 5) {
                    speedData.lastNonSpeedPos = to
                }
            }
        }

        speedData.lastMotion = motion

        if (to.y > from.y && shouldCheck(p, CheckType.SPEED)) {
            //boolean isSameXZ = from.x == to.x && from.z == to.z;
            //System.out.println("Y: " + (to.y - from.y));

            var maxSpeed = SpeedData.JUMP

            if (!cheatData.isInLiquid) {
                if (acData.collisionData.onClimbable && time - cheatData.lastJump < 1000) {
                    maxSpeed = SpeedData.LADDER
                }

                val diff = to.y - from.y
//                if (diff > maxVert) {
//                    maxVert = diff
//                }

                if (diff > maxSpeed) {

                    if (diff - maxSpeed > 0.5 && GTAnticheat.conf.enabled(CheckType.TELEPORT)) {
                        debug { "vert: $diff   max: $maxSpeed" }
                        e.setCancelled()
                        return false
                    }

                    if (time - cheatData.lastJump > 2000 && to.y - cheatData.lastJumpPos.y >= 1.2) {
                        if (speedData.successCount > 5) { //ignore it
                            speedData.successCount = 0
                            //data.speedData.wasRevert = true;
                        } else {
                            debug { "horizontal speed: " + (to.y - from.y) }
                            cheatData.speedPoints++
                            e.setCancelled()
                            return false
                        }
                    }
                }
            }
        }

        //System.out.println("expected distance: " + maxDistance + "    real: " + distance);

        if (cheatData.speedPoints > 4 && cheatData.speedPoints % 5 == 0) {
            p.sendAttributes()
        }

        return true
    }
}