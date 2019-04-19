package cz.creeperface.nukkit.gac.utils

import cn.nukkit.Player
import cn.nukkit.block.Block
import cn.nukkit.entity.Entity
import cn.nukkit.math.AxisAlignedBB
import cn.nukkit.math.Vector2
import cn.nukkit.math.Vector3
import cn.nukkit.utils.MainLogger
import cz.creeperface.nukkit.gac.GTAnticheat

/**
 * Created by CreeperFace on 3.8.2017.
 */
object CheatUtils {

    private val JUMP_MOTION = 0.44721359549996

    val NORMAL_JUMP: Double

    init {
        NORMAL_JUMP = calculateJumpHeight(null)
    }

    @JvmOverloads
    fun calculateJumpHeight(p: Entity?, motion: Double = JUMP_MOTION): Double {
        //double time = ticks * 60 + System.currentTimeMillis();
        return motion * motion / 0.16
    }

    fun insidePyramid(center: Vector3, rectangle: AxisAlignedBB, block: Block): Boolean {
        val triangle = block.boundingBox.clone() //triangle - convex polyhedron
        //triangle.maxY -= (triangle.maxY - triangle.minY) / 2;

        val xa = (triangle.minX + triangle.maxX) / 2
        val za = (triangle.minZ + triangle.maxZ) / 2
        val fastCheck = triangle.minY < rectangle.maxY && triangle.maxY > rectangle.minY && rectangle.minX < xa && rectangle.maxX > xa && rectangle.minZ < za && rectangle.maxZ > za
        if (fastCheck) {
            MainLogger.getLogger().warning("FAST CHECK")
            printBB(triangle, "block: ")
            printBB(rectangle, "player: ")
            return true
        }

        var t0: Vector2
        var t1: Vector2
        var t2: Vector2

        var v0: Vector2
        var v1: Vector2
        var v2: Vector2
        var v3: Vector2

        t0 = Vector2(triangle.minX, triangle.minY)
        t1 = Vector2(triangle.maxX, triangle.minY)
        t2 = Vector2(if (Math.abs(center.x - triangle.maxX) < Math.abs(center.x - triangle.minX)) triangle.minX else triangle.maxX /*(triangle.minX + triangle.maxX) / 2*/, rectangle.maxY)

        v0 = Vector2(rectangle.minX, rectangle.minY)
        v1 = Vector2(rectangle.minX, rectangle.maxY)
        v2 = Vector2(rectangle.maxX, rectangle.minY)
        v3 = Vector2(rectangle.maxX, rectangle.maxY)
        val r0 = pointInTriangle(v0, t0, t1, t2) || pointInTriangle(v1, t0, t1, t2) || pointInTriangle(v2, t0, t1, t2) || pointInTriangle(v3, t0, t1, t2)

        if (r0) {
            MainLogger.getLogger().warning("FIRST CHECK")
            printBB(triangle, "block: ")
            printBB(rectangle, "player: ")
            return true
        }

        t0 = Vector2(triangle.minZ, triangle.minY)
        t1 = Vector2(triangle.maxZ, triangle.minY)
        t2 = Vector2(if (Math.abs(center.z - triangle.maxZ) < Math.abs(center.z - triangle.minZ)) triangle.minZ else triangle.maxZ/*(triangle.minZ + triangle.maxZ) / 2*/, rectangle.maxY)

        v0 = Vector2(rectangle.minZ, rectangle.minY)
        v1 = Vector2(rectangle.minZ, rectangle.maxY)
        v2 = Vector2(rectangle.maxZ, rectangle.minY)
        v3 = Vector2(rectangle.maxZ, rectangle.maxY)

        if (pointInTriangle(v0, t0, t1, t2) || pointInTriangle(v1, t0, t1, t2) || pointInTriangle(v2, t0, t1, t2) || pointInTriangle(v3, t0, t1, t2)) {
            MainLogger.getLogger().warning("LAST CHECK")
            printBB(triangle, "block: ")
            printBB(rectangle, "player: ")
            return true
        }

        return false
    }

    private fun sign(p1: Vector2, p2: Vector2, p3: Vector2): Double {
        return (p1.x - p3.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p3.y)
    }

    private fun pointInTriangle(pt: Vector2, v1: Vector2, v2: Vector2, v3: Vector2): Boolean {
        val b1: Boolean = sign(pt, v1, v2) < 0.0f
        val b2: Boolean = sign(pt, v2, v3) < 0.0f
        val b3: Boolean = sign(pt, v3, v1) < 0.0f

        return b1 == b2 && b2 == b3
    }

    private fun printBB(bb: AxisAlignedBB, msg: String) {
        MainLogger.getLogger().info(msg + " (minX: " + bb.minX + "  minY: " + bb.minY + "  minZ: " + bb.minZ + "  maxX: " + bb.maxX + "  maxY: " + bb.maxY + "  maxZ: " + bb.maxZ + ")")
    }

    fun collidesWithNotFullBlock(center: Vector3, bb: AxisAlignedBB, block: AxisAlignedBB): Boolean {
        return !(block.minY <= bb.minY && block.maxY - bb.minY < 0.6)
    }
}

fun shouldCheck(entity: Entity, checkType: CheckType? = null): Boolean {
    return  GTAnticheat.instance.isLevelActive(entity.level) && (checkType == null || GTAnticheat.conf.enabled(checkType)) && entity is Player && (!entity.isOp || GTAnticheat.conf.checkOPs)
    //MainLogger.getLogger().info("check: $result (${GTAnticheat.instance.isLevelActive(entity.level)}, ${checkType == null || GTAnticheat.conf.enabled(checkType)}, ${entity is Player && (!entity.isOp || GTAnticheat.conf.checkOPs)})")

//    return result
}
