package cz.creeperface.nukkit.gac.utils

import cn.nukkit.block.BlockStairs
import cn.nukkit.math.AxisAlignedBB
import cn.nukkit.math.BlockFace
import cn.nukkit.math.NukkitMath
import cn.nukkit.math.SimpleAxisAlignedBB

/**
 * @author CreeperFace
 */

fun BlockStairs.getBoundingBoxes(): List<AxisAlignedBB> {
    val bbs = mutableListOf<AxisAlignedBB>()

    val top = this.isTop()

    if (top) {
        bbs.add(Stairs.AABB_SLAB_TOP)
    } else {
        bbs.add(Stairs.AABB_SLAB_BOTTOM)
    }

    val shape = this.getShape()

    if (shape == StairsShape.STRAIGHT || shape == StairsShape.INNER_LEFT || shape == StairsShape.INNER_RIGHT) {
        bbs.add(this.getCollQuarterBlock())
    }

    if (shape != StairsShape.STRAIGHT) {
        bbs.add(this.getCollEighthBlock())
    }

    return bbs.map { it.clone().offset(this.x, this.y, this.z) }
}

fun BlockStairs.getShape(): StairsShape {
    val facing = this.getFacing()
    val side = this.getSide(facing)
    val blockTop = this.isTop()

    if (side is BlockStairs && blockTop == side.isTop()) {
        val sideFacing = side.getFacing()

        if (sideFacing.axis !== facing.axis && this.isDifferent(sideFacing.opposite)) {
            return if (sideFacing === facing.rotateYCCW()) {
                StairsShape.OUTER_LEFT
            } else StairsShape.OUTER_RIGHT
        }
    }

    val side2 = this.getSide(facing.opposite)

    if (side2 is BlockStairs && blockTop == side2.isTop()) {
        val facing2 = side2.getFacing()

        if (facing2.axis != facing.axis && this.isDifferent(facing2)) {
            return if (facing2 === facing.rotateYCCW()) {
                StairsShape.INNER_LEFT
            } else StairsShape.INNER_RIGHT

        }
    }

    return StairsShape.STRAIGHT
}

fun BlockStairs.getFacing(): BlockFace {
    return BlockFace.fromIndex(5 - (this.damage and 3))
}

fun BlockStairs.isTop() = (this.damage and 4) > 0

fun BlockStairs.isDifferent(side: BlockFace): Boolean {
    val another = getSide(side)
    return another !is BlockStairs || another.getFacing() !== this.getFacing() || another.isTop() != this.isTop()

}

fun BlockStairs.getCollQuarterBlock(): AxisAlignedBB {
    val isTop = this.isTop()

    return when (this.getFacing()) {
        BlockFace.NORTH -> if (isTop) Stairs.AABB_QTR_BOT_NORTH else Stairs.AABB_QTR_TOP_NORTH

        BlockFace.SOUTH -> if (isTop) Stairs.AABB_QTR_BOT_SOUTH else Stairs.AABB_QTR_TOP_SOUTH

        BlockFace.WEST -> if (isTop) Stairs.AABB_QTR_BOT_WEST else Stairs.AABB_QTR_TOP_WEST

        BlockFace.EAST -> if (isTop) Stairs.AABB_QTR_BOT_EAST else Stairs.AABB_QTR_TOP_EAST
        else -> if (isTop) Stairs.AABB_QTR_BOT_NORTH else Stairs.AABB_QTR_TOP_NORTH
    }
}

fun BlockStairs.getCollEighthBlock(): AxisAlignedBB {
    val facing = this.getFacing()

    val facing1 = when (this.getShape()) {
        StairsShape.OUTER_LEFT -> facing

        StairsShape.OUTER_RIGHT -> facing.rotateY()

        StairsShape.INNER_RIGHT -> facing.opposite

        StairsShape.INNER_LEFT -> facing.rotateYCCW()
        else -> facing
    }

    val isTop = this.isTop()

    return when (facing1) {
        BlockFace.NORTH -> if (isTop) Stairs.AABB_OCT_BOT_NW else Stairs.AABB_OCT_TOP_NW

        BlockFace.SOUTH -> if (isTop) Stairs.AABB_OCT_BOT_SE else Stairs.AABB_OCT_TOP_SE

        BlockFace.WEST -> if (isTop) Stairs.AABB_OCT_BOT_SW else Stairs.AABB_OCT_TOP_SW

        BlockFace.EAST -> if (isTop) Stairs.AABB_OCT_BOT_NE else Stairs.AABB_OCT_TOP_NE
        else -> if (isTop) Stairs.AABB_OCT_BOT_NW else Stairs.AABB_OCT_TOP_NW
    }
}

fun AxisAlignedBB.mutable() = SimpleAxisAlignedBB(this.minX, this.minY, this.minZ, this.maxX, this.maxY, this.maxZ)

enum class StairsShape {
    STRAIGHT,
    INNER_LEFT,
    INNER_RIGHT,
    OUTER_LEFT,
    OUTER_RIGHT;
}

private object Stairs {
    /**
     * B: .. T: xx
     * B: .. T: xx
     */
    val AABB_SLAB_TOP = SimpleAxisAlignedBB(0.0, 0.5, 0.0, 1.0, 1.0, 1.0)

    /**
     * B: .. T: x.
     * B: .. T: x.
     */
    val AABB_QTR_TOP_WEST = SimpleAxisAlignedBB(0.0, 0.5, 0.0, 0.5, 1.0, 1.0)

    /**
     * B: .. T: .x
     * B: .. T: .x
     */
    val AABB_QTR_TOP_EAST = SimpleAxisAlignedBB(0.5, 0.5, 0.0, 1.0, 1.0, 1.0)

    /**
     * B: .. T: xx
     * B: .. T: ..
     */
    val AABB_QTR_TOP_NORTH = SimpleAxisAlignedBB(0.0, 0.5, 0.0, 1.0, 1.0, 0.5)

    /**
     * B: .. T: ..
     * B: .. T: xx
     */
    val AABB_QTR_TOP_SOUTH = SimpleAxisAlignedBB(0.0, 0.5, 0.5, 1.0, 1.0, 1.0)

    /**
     * B: .. T: x.
     * B: .. T: ..
     */
    val AABB_OCT_TOP_NW = SimpleAxisAlignedBB(0.0, 0.5, 0.0, 0.5, 1.0, 0.5)

    /**
     * B: .. T: .x
     * B: .. T: ..
     */
    val AABB_OCT_TOP_NE = SimpleAxisAlignedBB(0.5, 0.5, 0.0, 1.0, 1.0, 0.5)

    /**
     * B: .. T: ..
     * B: .. T: x.
     */
    val AABB_OCT_TOP_SW = SimpleAxisAlignedBB(0.0, 0.5, 0.5, 0.5, 1.0, 1.0)

    /**
     * B: .. T: ..
     * B: .. T: .x
     */
    val AABB_OCT_TOP_SE = SimpleAxisAlignedBB(0.5, 0.5, 0.5, 1.0, 1.0, 1.0)

    /**
     * B: xx T: ..
     * B: xx T: ..
     */
    val AABB_SLAB_BOTTOM = SimpleAxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.5, 1.0)

    /**
     * B: x. T: ..
     * B: x. T: ..
     */
    val AABB_QTR_BOT_WEST = SimpleAxisAlignedBB(0.0, 0.0, 0.0, 0.5, 0.5, 1.0)

    /**
     * B: .x T: ..
     * B: .x T: ..
     */
    val AABB_QTR_BOT_EAST = SimpleAxisAlignedBB(0.5, 0.0, 0.0, 1.0, 0.5, 1.0)

    /**
     * B: xx T: ..
     * B: .. T: ..
     */
    val AABB_QTR_BOT_NORTH = SimpleAxisAlignedBB(0.0, 0.0, 0.0, 1.0, 0.5, 0.5)

    /**
     * B: .. T: ..
     * B: xx T: ..
     */
    val AABB_QTR_BOT_SOUTH = SimpleAxisAlignedBB(0.0, 0.0, 0.5, 1.0, 0.5, 1.0)

    /**
     * B: x. T: ..
     * B: .. T: ..
     */
    val AABB_OCT_BOT_NW = SimpleAxisAlignedBB(0.0, 0.0, 0.0, 0.5, 0.5, 0.5)

    /**
     * B: .x T: ..
     * B: .. T: ..
     */
    val AABB_OCT_BOT_NE = SimpleAxisAlignedBB(0.5, 0.0, 0.0, 1.0, 0.5, 0.5)

    /**
     * B: .. T: ..
     * B: x. T: ..
     */
    val AABB_OCT_BOT_SW = SimpleAxisAlignedBB(0.0, 0.0, 0.5, 0.5, 0.5, 1.0)

    /**
     * B: .. T: ..
     * B: .x T: ..
     */
    val AABB_OCT_BOT_SE = SimpleAxisAlignedBB(0.5, 0.0, 0.5, 1.0, 0.5, 1.0)
}

inline fun AxisAlignedBB.forEachBlocks(action: (Int, Int, Int) -> Unit) {
    val minX = NukkitMath.floorDouble(this.minX)
    val minY = NukkitMath.floorDouble(this.minY)
    val minZ = NukkitMath.floorDouble(this.minZ)
    val maxX = NukkitMath.ceilDouble(this.maxX)
    val maxY = NukkitMath.ceilDouble(this.maxY)
    val maxZ = NukkitMath.ceilDouble(this.maxZ)

    for (z in minZ..maxZ) {
        for (x in minX..maxX) {
            for (y in minY..maxY) {
                action(x, y, z)
            }
        }
    }
}