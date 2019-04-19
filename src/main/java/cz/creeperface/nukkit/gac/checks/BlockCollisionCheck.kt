package cz.creeperface.nukkit.gac.checks

import cn.nukkit.block.*
import cn.nukkit.event.player.PlayerMoveEvent
import cn.nukkit.item.Item
import cn.nukkit.level.Level
import cn.nukkit.math.*
//import cn.nukkit.math.SimpleAxisAlignedBB
import cz.creeperface.nukkit.gac.GTAnticheat
import cz.creeperface.nukkit.gac.utils.getBoundingBoxes
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import java.util.*

/**
 * Created by CreeperFace on 7. 12. 2016.
 */
object BlockCollisionCheck {

    //private static BlockVector3 temporalVector = new BlockVector3();

    internal val collisionCalculators: Int2ObjectOpenHashMap<(AxisAlignedBB, Block) -> Boolean>


    fun run(e: PlayerMoveEvent, data: cz.creeperface.nukkit.gac.ACData, time: Long, checkNoClip: Boolean): Boolean {
        val p = e.player
        val from = e.from
        val to = e.to

        if (!e.isCancelled) {
            e.isResetBlocksAround = false
            val newBB = p.getBoundingBox()

            val bb2 = newBB.clone()
            bb2.expand(-0.2, 0.0, -0.2)

            //boolean checkSlab = true;
            //boolean complexCheck = false;

            when {
                to.y < from.y -> //checkSlab = false;
                    //bb2.minY += 0.; should we do something?
                    bb2.minY += 0.1
                to.y >= from.y -> {
                    bb2.maxY -= 0.1
                    bb2.minY += 0.1 //TODO check
                    //complexCheck = true;
                }
                else -> bb2.minY += 0.1
            }

            var water = false
            var climb = false
            var collision = false

            val cheatData = data.antiCheatData
            //double slabDistance = to.distance(cheatData.getLastSlab());

            val collisionData = data.collisionData

            var revert = false
            //BlockVector3 newFloorPos = temporalVector.setComponents(to.getFloorX(), to.getFloorY(), to.getFloorZ());

            //boolean isPosEqual = newFloorPos.equals(collisionData.lastFloorPos);
            val blocksAround = getBlocksAround(p.level, newBB)
            val collidingBlocks = getCollisionBlocks(blocksAround, newBB)

            //System.out.println("floorY: "+bb2.minY+"   "+p.y+"     "+newBB.minY+"          "+p.getBoundingBox().minY);

            for (block in collidingBlocks) {
                if (checkNoClip && /*!block.isTransparent() && */ !block.canPassThrough()) {
                    if (!revert) {
                        val bb = block.boundingBox
                        if (bb != null && bb.maxY - newBB.minY >= 0.6 && block.collidesWith(bb2)/* || CheatUtils.collidesWithNotFullBlock(p, bb2, bb)*//*!complexCheck || CheatUtils.insidePyramid(p, bb2, block))*//*(complexCheck && bb.maxY - p.y >= 0.45 && bb.minY <= p.y) ? CheatUtils.insidePyramid(p, bb2, block) : block.collidesWithBB(bb2)*/) {
                            //MainLogger.getLogger().info("BLOCK: "+block.getName());
                            //revert = !checkSlab || slabDistance > 1;
                            revert = true
                            //System.out.println("blocks: " + block.getName() + "    " + block.getId());
                            //System.out.println("PY: "+p.y+"      Y: "+bb2.minY);

                            //block.getBoundingBox().intersectsWith()
                            //AxisAlignedBB blockBB = block.getBoundingBox();
                            //System.out.println("collision horizontal: "+(bb2.maxX > blockBB.minX && bb2.minX < blockBB.maxX && bb2.maxZ > blockBB.minZ && bb2.minZ < blockBB.maxZ) +"       vertical: "+(bb2.maxY > blockBB.minY && bb2.minY < blockBB.maxY));
                        }
                    }

                    collision = true
                } else {
                    if (block is BlockLiquid) {
                        water = true
                    } else if (block.id == Item.LADDER || block.id == Item.VINE) {
                        if (NukkitMath.floorDouble(block.y) == NukkitMath.floorDouble(p.y)) {
                            climb = true
                        }
                    }/* else if (from.y <= to.y && block.getFloorY() == p.getFloorY() && (block instanceof BlockStairs || block instanceof BlockSlab)) {
                        cheatData.setLastSlab(p.clone());
                        cheatData.setLastSlabTime(time);
                    }*/
                }
            }

            if (revert) {
                e.to = collisionData.lastFreePos
                if (GTAnticheat.DEBUG) println("collision revert")
                return false
            }

            if (!collision) {
                collisionData.lastFreePos = to.clone()
            }

            p.blocksAround = blocksAround
            p.collisionBlocks = collidingBlocks

            collisionData.onClimbable = climb

            if (water) {
                cheatData.lastLiquid = to
                //System.out.println("water");

                if (!cheatData.isInLiquid) {
                    cheatData.enter = time
                }
            }

            cheatData.isInLiquid = water
        }

        return true
    }

    private fun getBlocksAround(level: Level, bb: AxisAlignedBB): List<Block> {
        val blocksAround = ArrayList<Block>()
        val vector3 = Vector3()

        val minX = NukkitMath.floorDouble(bb.minX)
        val minY = NukkitMath.floorDouble(bb.minY)
        val minZ = NukkitMath.floorDouble(bb.minZ)
        val maxX = NukkitMath.ceilDouble(bb.maxX)
        val maxY = NukkitMath.ceilDouble(bb.maxY)
        val maxZ = NukkitMath.ceilDouble(bb.maxZ)

        for (z in minZ..maxZ) {
            for (x in minX..maxX) {
                for (y in minY..maxY) {
                    val block = level.getBlock(vector3.setComponents(x.toDouble(), y.toDouble(), z.toDouble()))
                    blocksAround.add(block)
                }
            }
        }

        return blocksAround
    }

    private fun getCollisionBlocks(blocks: List<Block>, bb: AxisAlignedBB): List<Block> {
        val collisionBlocks = ArrayList<Block>()

        for (b in blocks) {
            if (b.collidesWith(bb, true)) {
                collisionBlocks.add(b)
            }
        }

        return collisionBlocks
    }

    init {
        val map = Int2ObjectOpenHashMap<(AxisAlignedBB, Block) -> Boolean>()

        val fence: (AxisAlignedBB, Block) -> Boolean = calc@{ bb, b ->
            b as BlockFence

            val north = b.canConnect(b.north())
            val south = b.canConnect(b.south())
            val west = b.canConnect(b.west())
            val east = b.canConnect(b.east())

            val northBB = SimpleAxisAlignedBB(b.x + 0.375, b.y, b.z + 0.375, b.x + 0.625, b.y + 1.5, b.z + 0.625)

            if (west || east) {
                val eastBB = northBB.clone()

                if (west) {
                    eastBB.minX -= 0.375
                }

                if (east) {
                    eastBB.maxX += 0.375
                }

                if (bb.intersectsWith(eastBB))
                    return@calc true
            }

            if (north) {
                northBB.minZ -= 0.375
            }

            if (south) {
                northBB.maxZ += 0.375
            }

            return@calc bb.intersectsWith(northBB)
        }

        val thin: (AxisAlignedBB, Block) -> Boolean = calc@{ bb, b ->
            b as BlockThin

            val north = b.canConnect(b.north())
            val south = b.canConnect(b.south())
            val west = b.canConnect(b.west())
            val east = b.canConnect(b.east())

            val northBB = SimpleAxisAlignedBB(b.x + 0.4375, b.y, b.z + 0.4375, b.x + 0.5625, b.y + 1, b.z + 0.5625)

            if (west || east) {
                val eastBB = northBB.clone()

                if (west) {
                    eastBB.minX -= 0.4375
                }

                if (east) {
                    eastBB.maxX += 0.4375
                }

                if (bb.intersectsWith(eastBB)) {
                    return@calc true
                }
            }

            if (north) {
                northBB.minZ -= 0.4375
            }

            if (south) {
                northBB.maxZ += 0.4375
            }

            return@calc bb.intersectsWith(northBB)
//                MainLogger.getLogger().info("north: ${bb.minX - northBB.maxX}, ${northBB.minX - bb.maxX}")
        }

        val trapdoor: (AxisAlignedBB, Block) -> Boolean = calc@{ bb, b ->
            b as BlockTrapdoor

            val blockbb = when {
                b.isOpen -> when(b.blockFace) {
                    BlockFace.NORTH -> SimpleAxisAlignedBB(0.0, 0.0, 0.8125, 1.0, 1.0, 1.0)
                    BlockFace.EAST -> SimpleAxisAlignedBB(0.0, 0.0, 0.0, 0.1875, 1.0, 1.0)
                    BlockFace.WEST -> SimpleAxisAlignedBB(0.8125, 0.0, 0.0, 1.0, 1.0, 1.0)
                    BlockFace.SOUTH -> SimpleAxisAlignedBB(0.0, 0.0, 0.0, 1.0, 1.0, 0.1875)
                    else -> SimpleAxisAlignedBB(0.0, 0.0, 0.8125, 1.0, 1.0, 1.0)
                }
                b.isTop -> SimpleAxisAlignedBB(0.0, 0.8125, 0.0, 1.0, 1.0, 1.0)
                else -> SimpleAxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.1875, 1.0)
            }

            blockbb.offset(b.x, b.y, b.z)

            return@calc bb.intersectsWith(blockbb)
        }

        val stairs: (AxisAlignedBB, Block) -> Boolean = calc@{ bb, b ->
            b as BlockStairs

            b.getBoundingBoxes().forEach {
                if(bb.intersectsWith(it)) return@calc true
            }

            return@calc false
        }

        map[Block.FENCE] = fence
        map[Block.NETHER_BRICK_FENCE] = fence

        map[Block.IRON_BARS] = thin
        map[Block.GLASS_PANE] = thin
        map[Block.STAINED_GLASS_PANE] = thin
        map[190] = thin //hard glass pane
        map[191] = thin //stained hard glass pane
        map[Block.TRAPDOOR] = trapdoor
        map[Block.IRON_TRAPDOOR] = trapdoor

        map[Block.ACACIA_WOODEN_STAIRS] = stairs
        map[Block.BIRCH_WOODEN_STAIRS] = stairs
        map[Block.BRICK_STAIRS] = stairs
        map[Block.COBBLESTONE_STAIRS] = stairs
        map[Block.DARK_OAK_WOODEN_STAIRS] = stairs
        map[Block.JUNGLE_WOODEN_STAIRS] = stairs
        map[Block.NETHER_BRICKS_STAIRS] = stairs
        map[Block.OAK_WOODEN_STAIRS] = stairs
        map[Block.PURPUR_STAIRS] = stairs
        map[Block.QUARTZ_STAIRS] = stairs
        map[Block.RED_SANDSTONE_STAIRS] = stairs
        map[Block.SANDSTONE_STAIRS] = stairs
        map[Block.SPRUCE_WOOD_STAIRS] = stairs
        map[Block.STONE_BRICK_STAIRS] = stairs

        map[Block.BREWING_STAND_BLOCK] = calc@{ bb, b ->
            //            val bb0 = SimpleAxisAlignedBB(b.x, b.y, b.z, b.x + 1.0, b.y + 0.125, b.z + 1.0);
//            val bb1 = SimpleAxisAlignedBB(b.x + 0.4375, b.y, b.z + 0.4375, b.x + 0.5625, b.y + 0.875, b.z + 0.5625)

            return@calc bb.intersectsWith(SimpleAxisAlignedBB(b.x, b.y, b.z, b.x + 1, b.y + 0.125, b.z + 1)) || bb.intersectsWith(SimpleAxisAlignedBB(b.x + 0.4375, b.y, b.z + 0.4375, b.x + 0.5625, b.y + 0.875, b.z + 0.5625))
        }

        map[Block.CAULDRON_BLOCK] = calc@{ bb, b ->
            //            val AABB_LEGS = SimpleAxisAlignedBB(b.x, b.y, b.z, b.x + 1, b.y + 0.3125, b.z + 1)
//            val AABB_WALL_NORTH = SimpleAxisAlignedBB(b.x, b.y, b.z, b.x + 1, b.y + 1, b.z + 0.125)
//            val AABB_WALL_SOUTH = SimpleAxisAlignedBB(b.x, b.y, b.z + 0.875, b.x + 1, b.y + 1, b.z + 1)
//            val AABB_WALL_EAST = SimpleAxisAlignedBB(b.x + 0.875, b.y, b.z, b.x + 1, b.y + 1, b.z + 1)
//            val AABB_WALL_WEST = SimpleAxisAlignedBB(b.x, b.y, b.z, b.x + 0.125, b.y + 1, b.z + 1)

            return@calc bb.intersectsWith(SimpleAxisAlignedBB(b.x, b.y, b.z, b.x + 1, b.y + 0.3125, b.z + 1)) ||
                    bb.intersectsWith(SimpleAxisAlignedBB(b.x, b.y, b.z, b.x + 1, b.y + 1, b.z + 0.125)) ||
                    bb.intersectsWith(SimpleAxisAlignedBB(b.x, b.y, b.z + 0.875, b.x + 1, b.y + 1, b.z + 1)) ||
                    bb.intersectsWith(SimpleAxisAlignedBB(b.x + 0.875, b.y, b.z, b.x + 1, b.y + 1, b.z + 1)) ||
                    bb.intersectsWith(SimpleAxisAlignedBB(b.x, b.y, b.z, b.x + 0.125, b.y + 1, b.z + 1))
        }

        this.collisionCalculators = map
    }

//    class CrossBlockCollisionCheck(private val offset: Double) {
//
//        fun calculate(bb: AxisAlignedBB, b: Block):
//
//    }
}

fun Block.collidesWith(bb: AxisAlignedBB, collision: Boolean = false): Boolean {
    BlockCollisionCheck.collisionCalculators[this.id]?.let {
        return it(bb, this)
    }

    return this.collidesWithBB(bb, collision)
}