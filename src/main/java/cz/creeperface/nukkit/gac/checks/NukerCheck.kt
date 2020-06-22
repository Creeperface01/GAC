package cz.creeperface.nukkit.gac.checks

import cn.nukkit.Player
import cn.nukkit.block.Block
import cn.nukkit.block.BlockID
import cn.nukkit.level.Location
import cn.nukkit.math.AxisAlignedBB
import cn.nukkit.math.SimpleAxisAlignedBB
import cn.nukkit.math.Vector3
import cn.nukkit.utils.BlockIterator
import cz.creeperface.nukkit.gac.utils.GACTimings
import cz.creeperface.nukkit.gac.utils.mutable
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet
import kotlin.math.*

object NukerCheck {

    fun run(p: Player, b: Block): Boolean = GACTimings.nukerCheck.use {
        val pos = p.add(0.0, p.eyeHeight.toDouble(), 0.0)

        var bb: AxisAlignedBB? = b.boundingBox?.mutable()

        if (bb == null) {
            bb = SimpleAxisAlignedBB(b.x, b.y, b.z, b.x + 1, b.y + 1, b.z + 1)
        }

        bb.expand(-0.01, -0.01, -0.01)

        val positions = HashSet<Vector3>()

        if (pos.x > bb.maxX || pos.x < bb.minX) {
            if (pos.x > b.x) {
                //SIDE EAST
                positions.add(Vector3(bb.maxX, bb.maxY, bb.maxZ))
                positions.add(Vector3(bb.maxX, bb.maxY, bb.minZ))
                positions.add(Vector3(bb.maxX, bb.minY, bb.minZ))
                positions.add(Vector3(bb.maxX, bb.minY, bb.maxZ))
            } else {
                //SIDE WEST
                positions.add(Vector3(bb.minX, bb.maxY, bb.maxZ))
                positions.add(Vector3(bb.minX, bb.maxY, bb.minZ))
                positions.add(Vector3(bb.minX, bb.minY, bb.minZ))
                positions.add(Vector3(bb.minX, bb.minY, bb.maxZ))
            }
        }

        if (pos.z > bb.maxZ || pos.z < bb.minZ) {
            if (pos.z > b.z) {
                //SIDE SOUTH
                positions.add(Vector3(bb.minX, bb.maxY, bb.maxZ))
                positions.add(Vector3(bb.maxX, bb.minY, bb.maxZ))
                positions.add(Vector3(bb.minX, bb.minY, bb.maxZ))
                positions.add(Vector3(bb.maxX, bb.maxY, bb.maxZ))
            } else {
                //SIDE NORTH
                positions.add(Vector3(bb.minX, bb.maxY, bb.minZ))
                positions.add(Vector3(bb.maxX, bb.minY, bb.minZ))
                positions.add(Vector3(bb.minX, bb.minY, bb.minZ))
                positions.add(Vector3(bb.maxX, bb.maxY, bb.minZ))
            }
        }

        if (pos.y > bb.maxY || pos.y < bb.minY) {
            if (pos.y > b.y) {
                //SIDE UP
                positions.add(Vector3(bb.minX, bb.maxY, bb.minZ))
                positions.add(Vector3(bb.maxX, bb.maxY, bb.minZ))
                positions.add(Vector3(bb.minX, bb.maxY, bb.maxZ))
                positions.add(Vector3(bb.maxX, bb.maxY, bb.maxZ))
            } else {
                //SIDE DOWN
                positions.add(Vector3(bb.minX, bb.minY, bb.minZ))
                positions.add(Vector3(bb.maxX, bb.minY, bb.minZ))
                positions.add(Vector3(bb.minX, bb.minY, bb.maxZ))
                positions.add(Vector3(bb.maxX, bb.minY, bb.maxZ))
            }
        }

        if (positions.isEmpty()) { //inside the block probably
            return true
        }

        for (corner in positions) {
            val x = corner.x - pos.x
            val y = corner.y - pos.y
            val z = corner.z - pos.z

            val diff = abs(x) + abs(z)

            val yaw = Math.toDegrees(-atan2(x / diff, z / diff))
            val pitch = if (y == 0.toDouble()) 0.toDouble() else Math.toDegrees(-atan2(y, sqrt(x * x + z * z)))
            val found = getTargetBlock(
                    Location(pos.x, pos.y, pos.z, yaw, pitch, pos.getLevel()),
                    b,
                    ceil(corner.distance(pos) + 2).toInt(),
                    BlockID.AIR
            )

            if (corner.distanceSquared(pos) <= 0.25 || found == b) {
                return true
            } else if (found == null) {
                return false
            }
        }

        return false
    }

    private fun getTargetBlock(pos: Location, target: Vector3, maxDistance: Int, vararg transparent: Int): Block? {
        try {
            val blocks = rayTrace(pos, target, maxDistance, 1)

            val block = blocks[0]

            if (transparent.isNotEmpty()) {
                if (Arrays.binarySearch(transparent, block.id) < 0) {
                    return block
                }
            } else {
                return block
            }
        } catch (ignored: Exception) {

        }

        return null
    }

    private fun rayTrace(
            pos: Location,
            target: Vector3,
            maxDistance: Int,
            maxLength: Int
    ): Array<Block> {
        val blocks = ArrayList<Block>()

        val direction = pos.directionVector
        val itr = BlockIterator(pos.getLevel(), pos, direction, 0.0, min(15, maxDistance))

        while (itr.hasNext()) {
            val block = itr.next()
            blocks.add(block)

            if (maxLength != 0 && blocks.size > maxLength) {
                blocks.removeAt(0)
            }

            if (!block.isTransparent || block == target) {
                break
            }
        }

        return blocks.toTypedArray()
    }
}