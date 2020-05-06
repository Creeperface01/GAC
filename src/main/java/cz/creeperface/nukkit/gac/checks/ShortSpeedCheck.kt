package cz.creeperface.nukkit.gac.checks

import cn.nukkit.AdventureSettings.Type
import cn.nukkit.event.player.PlayerMoveEvent
import cn.nukkit.math.Vector2
import cz.creeperface.nukkit.gac.GTAnticheat
import cz.creeperface.nukkit.gac.checks.data.SpeedData
import cz.creeperface.nukkit.gac.utils.CheckType
import cz.creeperface.nukkit.gac.utils.debug

/**
 * Created by CreeperFace on 19. 11. 2016.
 */
object ShortSpeedCheck {

    fun run(e: PlayerMoveEvent, acData: cz.creeperface.nukkit.gac.ACData): Boolean {
        val p = e.player

        val time = System.currentTimeMillis()

        if (p.adventureSettings.get(Type.ALLOW_FLIGHT) || p.riding != null || !acData.motionData.isEmpty || time - acData.antiCheatData.lastTeleport < 2000 || acData.antiCheatData.isTeleport) {
            //System.out.println("speed check motion return");
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


        val speedData = acData.speedData
        if (from.x != to.x || from.z != to.z) {
            val fromv = Vector2(from.x, from.z)


            var maxDistance = 0.23

            if (p.adventureSettings.get(Type.FLYING)) {
                //System.out.println("fly ignore");
                return true
            } else if (p.isSprinting || acData.speedData.lastSpeedType == SpeedData.SpeedType.SPRINT && time - acData.speedData.lastSpeedChange < 800) {
                maxDistance = 0.32

                if (time - cheatData.lastJump < 1000) {
                    maxDistance += 0.2
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

            maxDistance *= (10 * p.movementSpeed).toDouble()

            val distance = fromv.distance(to.x, to.z)

//            if(inWater)
//                MainLogger.getLogger().info("water_distance: $distance")

            //System.out.println(distance+"   :   "+maxDistance+"     Y: "+Math.abs(to.y - from.y));
//            if(p.isSwimming) {
//                MainLogger.getLogger().info("swim distance: $distance")
//            }

            if (distance > maxDistance && GTAnticheat.conf.enabled(CheckType.SPEED)) {
                cheatData.speedPoints++
                speedData.successCount = 0
                //System.out.println(distance+"   :   "+maxDistance+"     Y: "+Math.abs(to.y - from.y));

                val diff = distance - maxDistance

                if (diff > 0.5 || cheatData.speedPoints > 10) {
                    debug { "horiz: $diff" }
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


        if (to.y > from.y && GTAnticheat.conf.enabled(CheckType.SPEED)) {
            //boolean isSameXZ = from.x == to.x && from.z == to.z;
            //System.out.println("Y: " + (to.y - from.y));

            var maxSpeed = SpeedData.JUMP

            if (!cheatData.isInLiquid) {
                if (acData.collisionData.onClimbable && time - cheatData.lastJump < 1000) {
                    maxSpeed = SpeedData.LADDER
                }

                val diff = to.y - from.y

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