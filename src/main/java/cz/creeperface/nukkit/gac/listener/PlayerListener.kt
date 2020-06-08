package cz.creeperface.nukkit.gac.listener

import cn.nukkit.AdventureSettings
import cn.nukkit.Player
import cn.nukkit.block.Block
import cn.nukkit.block.BlockSlab
import cn.nukkit.block.BlockStairs
import cn.nukkit.event.EventHandler
import cn.nukkit.event.EventPriority
import cn.nukkit.event.Listener
import cn.nukkit.event.block.BlockBreakEvent
import cn.nukkit.event.entity.*
import cn.nukkit.event.player.*
import cn.nukkit.item.Item
import cn.nukkit.item.enchantment.Enchantment
import cn.nukkit.math.Vector2
import cn.nukkit.math.Vector3
import cn.nukkit.potion.Effect
import cn.nukkit.utils.MainLogger
import cz.creeperface.nukkit.gac.GTAnticheat
import cz.creeperface.nukkit.gac.NoCheatTask
import cz.creeperface.nukkit.gac.checks.BlockCollisionCheck
import cz.creeperface.nukkit.gac.checks.NukerCheck
import cz.creeperface.nukkit.gac.checks.ShortSpeedCheck
import cz.creeperface.nukkit.gac.checks.collidesWith
import cz.creeperface.nukkit.gac.checks.data.SpeedData
import cz.creeperface.nukkit.gac.player.ICheatPlayer
import cz.creeperface.nukkit.gac.player.NukkitCheatPlayer
import cz.creeperface.nukkit.gac.utils.*
import kotlin.math.*

/**
 * @author CreeperFace
 */
class PlayerListener(private val plugin: GTAnticheat) : Listener {

    init {
        registerEvent(this, plugin, PlayerMoveEvent::class.java, { onMove(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, EntityMotionEvent::class.java, { onMotion(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, EntityLevelChangeEvent::class.java, { onLevelChange(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, PlayerTeleportEvent::class.java, { onTeleport(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, PlayerRespawnEvent::class.java, { onRespawn(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, PlayerToggleSprintEvent::class.java, { onSprint(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, PlayerToggleSneakEvent::class.java, { onSneak(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, PlayerToggleSwimEvent::class.java, { onSwim(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, BlockBreakEvent::class.java, { onBlockBreak(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, EntityDamageEvent::class.java, { onEntityDamage(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, PlayerJoinEvent::class.java, { onJoin(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, PlayerPreLoginEvent::class.java, { onPreLogin(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, PlayerCreationEvent::class.java, { onPlayerCreate(it) }, true, EventPriority.MONITOR)
        registerEvent(this, plugin, PlayerInvalidMoveEvent::class.java, { it.setCancelled() }, true, EventPriority.HIGHEST)
        registerEvent(this, plugin, PlayerToggleGlideEvent::class.java, { onPlayerGlide(it) }, true, EventPriority.MONITOR)
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun onMove(e: PlayerMoveEvent) {
        val p = e.player

        if (p !is ICheatPlayer || !shouldCheck(p)) {
            return
        }

        val from = e.from
        val to = p.currentPos

        val acData = p.acData

        var revert: Boolean

        acData.fakePlayer.update(from, to)
        val cheatData = acData.antiCheatData
        //System.out.println(this.getServer().getTick());

        if (p.riding != null || p.isGliding) {
            cheatData.lastOnGround = System.currentTimeMillis()
            cheatData.lastGroundPos = p.currentPos
            cheatData.isOnGround = true
//            debug { "set ground pos (riding)" }

            cheatData.horizontalFlightPoints = 0
            cheatData.flyPoints = 0
        }

        if (!p.checkGamemode() || to.x == from.x && to.y == from.y && to.z == from.z || p.adventureSettings.get(AdventureSettings.Type.FLYING) || p.riding != null || p.isGliding) {
            return
        }

        //System.out.println("distance: "+new Vector2(from.x, from.z).distance(to.x, to.z)+"     horizontal: "+Math.abs(to.y - from.y));

        //System.out.println("motion: "+p.getMotion());
        //double radius = (double)p.getWidth() / 2.0D;
        //AxisAlignedBB bb = temporalBB.setBounds(to.x - radius, to.y, to.z - radius, to.x + radius, to.y + (double)p.getHeight(), to.z + radius);

        //System.out.println("ground: "+p.onGround);
        p.checkGroundState(false)
        cheatData.largeGroundStateCheck = false
        //System.out.println("ground2: "+p.onGround);

        val time = System.currentTimeMillis()

//        if (time - acData.antiCheatData.lastTeleport < 5000) {
        //System.out.println("tp distance: "+from.distance(to)+"   tp: "+data.antiCheatData.teleportPosition.distance(to));
//        }

        if (cheatData.isTeleport) {
            cheatData.isTeleport = false
            BlockCollisionCheck.run(e, acData, time, false)
            cheatData.lastPos = to
            cheatData.lastGroundPos = to
//            debug { "set ground pos (teleport)" }
            return
        }

        revert = GTAnticheat.conf.enabled(CheckType.NOCLIP) && !BlockCollisionCheck.run(e, acData, time, true)

        //int sPoints = data.antiCheatData.speedPoints;
        if (!ShortSpeedCheck.run(e, acData)) {
            revert = true
            //System.out.println("revert speed");
        }

        GACTimings.flyCheck.execute {
            val bb = p.getBoundingBox().clone()

            val bb2 = bb.clone()
            bb2.minY = bb2.minY + 0.6
            bb2.expand(-0.2, 0.0, -0.2)

            val pLoc = p.currentPos

            val motionData = acData.motionData
            if (!motionData.isEmpty && !motionData.ground) {
                //System.out.println("!empty ground check");
                val timeY = motionData.timeY

                if (timeY > time && timeY - time > 1500 || time > timeY) {
                    if (motionData.groundTime == (-1).toLong()) {
                        if (p.onGround)
                            motionData.groundTime = time
                    } else if (time - motionData.groundTime > 1500) {
                        //System.out.println("motion ground");
                        motionData.ground = true
                    }
                }
            }

            val moveUp = to.y >= from.y

            if (!moveUp) {
                cheatData.lastDownMove = time
                //System.out.println("down");
            }

            if (cheatData.isLastPacketOnGround) {
                cheatData.lastOnGround = time
            }

            cheatData.isLastPacketOnGround = p.onGround

            if (revert || p.onGround || acData.collisionData.onClimbable || p.riding != null || p.adventureSettings.get(AdventureSettings.Type.FLYING) || !NoCheatTask.isInAir(p, acData.antiCheatData)) {
                if (p.onGround) {
                    cheatData.lastOnGround = time
                    cheatData.lastGroundPos = pLoc
                    cheatData.isOnGround = true
//                debug { "set ground pos (ground)" }
                }

                cheatData.horizontalFlightPoints = 0
                cheatData.flyPoints = 0
            } else {
                cheatData.isOnGround = false

                val inAirTime = (time - cheatData.lastOnGround).toInt()


                var glide = false

                if (GTAnticheat.conf.enabled(CheckType.GLIDE) && !moveUp && from.y - to.y < 1 && inAirTime > 2000) {
                    val expectedMotionY = (p.highestPosition - to.y) / ((time - acData.antiCheatData.lastHighestPos) / 50) * 0.06
                    val expectedY = p.highestPosition - to.y - 0.06 * ((time - acData.antiCheatData.lastHighestPos) / 50)
                    glide = inAirTime > 3000 && time - cheatData.lastJump > 3000 && (from.y - to.y >= expectedMotionY || to.y > expectedY) && to.y - expectedY > 5
                }

//            debug { "glide: $glide  moveUp: $moveUp" }
                if (to.y > -20 && ((GTAnticheat.conf.enabled(CheckType.FLY) && moveUp) || glide)) {
//                debug { "check fly" }
                    if (!acData.motionData.isEmpty && !acData.motionData.ground) {

                        val v = Vector2(motionData.fromX, motionData.fromZ)
                        val v2 = Vector2(motionData.x, motionData.z)

                        val pDistance = Vector2(p.x, p.z).distance(v2)

                        val expectedTime = motionData.timeY
                        val expectedY = motionData.y

                        if (motionData.groundTime == (-1).toLong() && (time - expectedTime > 1000 || to.y > expectedY + 1.5 || v.distance(v2) + 2 < pDistance)) {
                            //System.out.println("from: "+v.toString()+"       pPos: "+p.getPosition().toString());
                            //System.out.println("revert, currY: "+to.y+"  maxY: "+motionData.get("y")+"  distance: "+v.distance(v2)+"   pDistance: "+pDistance);
                            e.setCancelled()
                            debug { "revert motion" }
                        }
                    } else {
                        val pos = cheatData.lastGroundPos

                        //System.out.println("Y: "+(to.y - data.getLastGroundPos().y)+"          time: "+(data.getLastOnGround() - data.getLastJump()));

                        if (to.y - cheatData.lastGroundPos.y <= 1 && cheatData.lastOnGround > cheatData.lastJump && time - cheatData.lastPacketJump < 1000) {
                            plugin.onJump(p, acData)
                            //System.out.println("jump2");
                        }

                        val lastJumpPos = cheatData.lastJumpPos
                        val jumpDistance = lastJumpPos.distance(to)

//                    val slab = false

                        for (block in p.getBlocksUnder(null)) {
                            if ((block is BlockSlab || block is BlockStairs) && block.collidesWith(bb2)) {
                                cheatData.lastSlab = to
                                cheatData.lastSlabTime = time
                                //slab = true;
                            }

                            if (block.id == Item.LADDER || block.id == Item.VINE) {
                                //data.collisionData.onClimbable = true;
                                cheatData.isOnGround = true
                                cheatData.lastOnGround = time
                                cheatData.lastGroundPos = p.currentPos
//                            debug { "set ground pos (climbable)" }
                            }
                        }

                        //System.out.println(slab ? "!ground" : "!ground slab");


                        //double groundDistance = Math.sqrt(Math.pow(pos.x - to.x, 2.0D) + Math.pow(pos.z - to.z, 2.0D));
                        val groundDistance = pos.distance(to)
                        //double groundXZ = Math.sqrt(Math.pow(pos.x - to.x, 2.0D) + Math.pow(pos.z - to.z, 2.0D));

                        val slabDistance = sqrt((cheatData.lastSlab.x - to.x).pow(2.0) + (cheatData.lastSlab.z - to.z).pow(2.0))

                        val slabDistanceY = to.y - cheatData.lastSlab.y

                        //System.out.println("ground distance: "+groundDistance + "           inAirTime: "+inAirTime);
                        //System.out.println("liquid distance: "+data.getLastLiquid().distance(to));
                        //System.out.println("slab distance: "+slabDistance);

                        val waterDistance = sqrt((cheatData.lastLiquid.x - to.x).pow(2.0) + (cheatData.lastLiquid.z - to.z).pow(2.0))
                        val lastLiquid = cheatData.lastLiquid
                        lastLiquid.y = ceil(lastLiquid.y)
                        val waterDiff = to.subtract(lastLiquid)

                        if (
                                (slabDistance > 0.4 || slabDistanceY > 0.6 || time - cheatData.lastSlabTime > 500)
                                && groundDistance > 0.6 && (waterDistance > 0.6 || (waterDiff.y > 0 && abs(waterDiff.x) > 0.1 && abs(waterDiff.z) > 0.1) || waterDiff.y > 0.72)
                        ) {
//                        debug { "waterDiff: $waterDiff" }
                            //System.out.println("check 1");
                            //System.out.println("ground distance: "+groundDistance);


                            if (
                                    time - cheatData.lastHit > 2000 && (
                                            time - cheatData.lastJump > 3000
                                                    || jumpDistance > 3.2 && to.y >= lastJumpPos.y
                                                    || to.y - lastJumpPos.y > CheatUtils.calculateJumpHeight(p) * 1.1
                                                    || inAirTime > 3000 && cheatData.lastOnGround - cheatData.lastCheck <= 0
                                            )
                            ) {
                                if (GTAnticheat.DEBUG) {
                                    val check1 = time - cheatData.lastHit > 3000
                                    val check2 = time - cheatData.lastJump > 3000
                                    val check3 = jumpDistance > 3.2
                                    val check4 = to.y >= lastJumpPos.y
                                    val check5 = to.y - lastJumpPos.y > CheatUtils.calculateJumpHeight(p) * 1.1
                                    val check6 = inAirTime > 3000 && cheatData.lastOnGround - cheatData.lastCheck <= 0

                                    if (check1) {
                                        if (check2) {
                                            debug { "check2" }
                                        }

                                        if (check3) {
                                            debug { "check3" }
                                        }

                                        if (check4) {
                                            debug { "check4" }
                                        }

                                        if (check5) {
                                            debug { "check5" }
                                        }

                                        if (check6) {
                                            debug { "check6" }
                                        }
                                    }
                                }

                                //System.out.println("check 2");

                                if (pos.y >= to.y || glide) {

                                    debug { "normal revert 1" }
                                    if (pos.y - to.y <= 3) {
                                        e.to = cheatData.lastGroundPos
                                        //System.out.println("revert normal 1");
                                    } else {
                                        acData.antiCheatData.horizontalFlightPoints++
                                        e.setCancelled()
                                    }

                                    p.adventureSettings.set(AdventureSettings.Type.ALLOW_FLIGHT, false)
                                    p.adventureSettings.set(AdventureSettings.Type.FLYING, false)
                                    p.adventureSettings.update()
                                    p.motion = p.temporalVector.setComponents(0.0, -500.0, 0.0)
                                } else {
                                    debug { "revert normal 2" }
//                                    println("water check: ${waterDiff.x} ${waterDiff.z}    ${waterDiff.y}")
//                                    println("liquidY: ${lastLiquid.y}  ${to.y}")
                                    //System.out.println("checks:"+check2+", "+(check3 && check4)+", "+check5+", "+check6);

//                                if ((waterDiff.x == 0.0 || waterDiff.z == 0.0) && waterDiff.y < 0.72) {
//                                    debug { "waterY: ${waterDiff.y}" }
//                                } else {
                                    e.to = cheatData.lastGroundPos
                                    p.motion = p.temporalVector.setComponents(0.0, -500.0, 0.0)
                                    cheatData.lastOnGround = time
                                    p.adventureSettings.set(AdventureSettings.Type.ALLOW_FLIGHT, false)
                                    p.adventureSettings.set(AdventureSettings.Type.FLYING, false)
                                    p.adventureSettings.update()
//                                }
                                }


                                if (acData.antiCheatData.horizontalFlightPoints > 40) {
                                    p.kick(Messages.translate("kick_player", "fly", p.displayName), false)

                                    plugin.server.broadcastMessage(Messages.translate("kick_broadcast", "fly", p.displayName))
                                }

                                //}
                            }
                        }
                    }
                } else if (!acData.motionData.isEmpty) {
                    if (motionData.time < time && motionData.timeY < time) {
                        motionData.clear()
                        acData.speedData.lastNonSpeedPos = to
                        //System.out.println("clear 1");
                    }
                }
            }

            if (e.isCancelled) {
                //data.speedData.wasRevert = true;
                //data.speedData.lastRevert = time;
                acData.speedData.lastNonSpeedPos = from
                //System.out.println("revert");
            }

            if (!e.isCancelled && (p.onGround || to.y > p.highestPosition)) {
                //p.highestPosition = to.y;
                acData.antiCheatData.lastHighestPos = time
            }

            plugin.doKickCheck(acData, p)
            cheatData.lastCheck = time
        }
    }

    @EventHandler
    private fun onMotion(e: EntityMotionEvent) {
        val entity = e.entity

        if (entity is Player && entity is ICheatPlayer && shouldCheck(entity, CheckType.FLY)) {

            val p = entity as Player
            val motion = e.motion

            if (abs(motion.x) < 0.1 && motion.y <= 0 && abs(motion.z) < 0.1) {
                //System.out.println("motion event: "+e.getMotion());
                return
            }

            val motionData = (entity as ICheatPlayer).acData.motionData
            motionData.clear()

            val ticks = ceil(motion.y / 0.08).toInt()
            //double time = ticks * 60 + System.currentTimeMillis();

            val maxY = CheatUtils.calculateJumpHeight(p, motion.y) * 1.1

            //double drag = 0.02;

            val motionX = motion.x
            val motionZ = motion.z

            //int count = 0;
            //System.out.println("before: "+System.currentTimeMillis());

            //timeX = (motionX - 0.1) / SpeedData.friction;
            //timeZ = (motionZ - 0.1) / SpeedData.friction;

            //x = (motionX * timeX) - ((SpeedData.friction * timeX * timeX) / 2);
            //z = (motionZ * timeZ) - ((SpeedData.friction * timeZ * timeZ) / 2);

            val timeX = ln(0.98 * 0.2 / abs(motionX)) / ln(0.98)
            val timeZ = ln(0.98 * 0.2 / abs(motionZ)) / ln(0.98)

            val x = motionX * ((1 - 0.98.pow(timeX)) / (1 - 0.98))
            val z = motionZ * ((1 - 0.98.pow(timeZ)) / (1 - 0.98))

            //System.out.println("timeX: "+timeX+"  timeZ: "+timeZ+"   dX: "+x+"    dZ: "+z);


            val timeXZ = max(timeX, timeZ)

            //System.out.println("after: "+System.currentTimeMillis()+"    count: "+count);

            //System.out.println("maxY: "+p.y + maxY);

            val time = System.currentTimeMillis().toDouble() + timeXZ * 50 + 1000.0

            //System.out.println("time: "+time);

            motionData.time = time.toLong()
            motionData.timeY = System.currentTimeMillis() + ticks * 55
            motionData.y = p.y + maxY
            motionData.x = p.x + x
            motionData.z = p.z + z
            motionData.fromX = p.x
            motionData.fromZ = p.z
            motionData.distance = p.distance(Vector3(x, p.y, z))
            motionData.ground = false
        }
    }

    @EventHandler
    private fun onLevelChange(e: EntityLevelChangeEvent) {
        val ent = e.entity

        if (ent is ICheatPlayer && shouldCheck(ent)) {
            val data = (ent as ICheatPlayer).acData

            //data.antiCheatData.setLastTeleport(System.currentTimeMillis());
            data.antiCheatData.isTeleport = true
            //data.fakePlayer.update(e.get);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    private fun onTeleport(e: PlayerTeleportEvent) {
        if (!shouldCheck(e.player, CheckType.FLY)) {
            return
        }

        val p = e.player as? ICheatPlayer ?: return

        val data = p.acData
        data.speedData.lastNonSpeedPos = e.to

        if (e.cause == PlayerTeleportEvent.TeleportCause.PLUGIN) {
            val time = System.currentTimeMillis()

            data.antiCheatData.lastTeleport = time
            data.antiCheatData.isTeleport = true
            data.fakePlayer.update(e.to)
            data.antiCheatData.teleportPosition = e.to
            data.antiCheatData.lastGroundPos = e.to
            data.antiCheatData.lastOnGround = time
//            debug { "set ground pos (teleport event)" }
        }

    }

    @EventHandler
    private fun onRespawn(e: PlayerRespawnEvent) {
        if (!shouldCheck(e.player)) {
            return
        }

        val p = e.player as? ICheatPlayer ?: return


        //data.antiCheatData.setLastTeleport(System.currentTimeMillis());
        p.acData.antiCheatData.isTeleport = true
    }

    @EventHandler
    private fun onSprint(e: PlayerToggleSprintEvent) {
        val p = e.player

        debug { "sprint" }
        if (p !is ICheatPlayer || !shouldCheck(p, CheckType.SPEED)) {
            return
        }

        val data = p.acData.speedData

        data.lastSpeedChange = System.currentTimeMillis()
        data.lastSpeedType = data.currentSpeedType

        if (e.isSprinting) {
            data.currentSpeedType = SpeedData.SpeedType.SPRINT
        } else {
            data.currentSpeedType = SpeedData.SpeedType.WALK
        }
    }

    @EventHandler
    private fun onSneak(e: PlayerToggleSneakEvent) {
        val p = e.player

        if (p !is ICheatPlayer || shouldCheck(p, CheckType.SPEED)) {
            return
        }

        val data = p.acData.speedData

        data.lastSpeedChange = System.currentTimeMillis()
        data.lastSpeedType = data.currentSpeedType

        if (e.isSneaking) {
            data.currentSpeedType = SpeedData.SpeedType.SNEAK
        } else {
            data.currentSpeedType = SpeedData.SpeedType.WALK
        }
    }

    @EventHandler
    private fun onSwim(e: PlayerToggleSwimEvent) {
        val p = e.player

        if (p !is ICheatPlayer || !shouldCheck(p, CheckType.SPEED)) {
            return
        }

        val data = p.acData.speedData

        data.lastSpeedChange = System.currentTimeMillis()
        data.lastSpeedType = data.currentSpeedType

        if (e.isSwimming) {
            data.currentSpeedType = SpeedData.SpeedType.SWIM
        } else {
            data.currentSpeedType = SpeedData.SpeedType.WALK
        }
    }

    private fun checkSpeedMine(block: Block, data: cz.creeperface.nukkit.gac.ACData): Boolean {
        val player = data.player
        val cheatData = data.antiCheatData

        val item = player.inventory.itemInHand
        var breakTime = block.getBreakTime(item, player)

        if (player.isCreative && breakTime > 0.15) {
            breakTime = 0.15
        }

        if (player.hasEffect(Effect.HASTE)) {
            breakTime *= 1 - 0.2 * (player.getEffect(Effect.HASTE).amplifier + 1)
        }

        if (player.hasEffect(Effect.MINING_FATIGUE)) {
            breakTime *= 1 - 0.3 * (player.getEffect(Effect.MINING_FATIGUE).amplifier + 1)
        }

        val eff = item.getEnchantment(Enchantment.ID_EFFICIENCY)

        if (eff != null && eff.level > 0) {
            breakTime *= 1 - 0.3 * eff.level
        }

        breakTime -= 0.15
        breakTime *= 1000.0

        return cheatData.lastBlockBreak + breakTime > System.currentTimeMillis()
    }

    @EventHandler(ignoreCancelled = true)
    private fun onBlockBreak(e: BlockBreakEvent) {
        val p = e.player

        if (!p.checkGamemode() || p !is ICheatPlayer || !shouldCheck(p)) {
            return
        }

        val data = (p as ICheatPlayer).acData
        val b = e.block

        val time = System.currentTimeMillis()
        if (GTAnticheat.conf.enabled(CheckType.SPEEDMINE) && (e.isFastBreak || checkSpeedMine(b, data))) {
            data.antiCheatData.speedminePoints++
            p.lastBreak = time
            e.setCancelled()
        } else {
            val cheatData = data.antiCheatData

            if (cheatData.speedminePoints > 0) {
                cheatData.speedminePoints--
            }
        }

        if (GTAnticheat.conf.enabled(CheckType.NUKER) && !NukerCheck.run(p, b)) {
            e.setCancelled()
            data.antiCheatData.nukerPoints++
        } else {
            if (data.antiCheatData.nukerPoints > 0) {
                data.antiCheatData.nukerPoints--
            }
        }

        data.antiCheatData.lastBlockBreak = time
    }

    @EventHandler(ignoreCancelled = true)
    private fun onEntityDamage(e: EntityDamageEvent) {
        val entity = e.entity

        if (!shouldCheck(entity))
            return

        if (e is EntityDamageByEntityEvent && e !is EntityDamageByChildEntityEvent) {
            val damager = e.damager
            if (damager is Player) {

                if (!damager.checkGamemode()) {
                    return
                }
                //aData.fakePlayer.update(attacker.getLocation());

                if (GTAnticheat.conf.enabled(CheckType.REACH) && !NoCheatTask.rangeCheck(entity.getBoundingBox(), damager, GTAnticheat.conf.hitRange)) {
                    e.setCancelled()
                    return
                }
            }
        }
    }

    private fun onPreLogin(e: PlayerPreLoginEvent) {
        val p = e.player
        val data = p.skin.skinData.data

        val count = data.count { it.toInt() == 0 }

        MainLogger.getLogger().info("count: ${data.size} transparent: $count")
    }

    @EventHandler(priority = EventPriority.LOWEST)
    private fun onJoin(e: PlayerJoinEvent) {
        val p = e.player

        if (p !is ICheatPlayer) {
            return
        }

        p.acData.fakePlayer.init(p)
    }

    @EventHandler
    private fun onPlayerCreate(e: PlayerCreationEvent) {
        e.playerClass = NukkitCheatPlayer::class.java
    }

    @EventHandler
    private fun onPlayerGlide(e: PlayerToggleGlideEvent) {
        val p = e.player

        if (p !is ICheatPlayer) {
            return
        }

        if (!GTAnticheat.conf.enableElytra) {
            e.setCancelled()
        }
    }
}
