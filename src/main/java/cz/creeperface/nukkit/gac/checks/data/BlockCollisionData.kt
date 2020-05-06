package cz.creeperface.nukkit.gac.checks.data

import cn.nukkit.level.Location
import cn.nukkit.math.BlockVector3
import cn.nukkit.math.Vector3

/**
 * Created by CreeperFace on 7. 12. 2016.
 */
class BlockCollisionData(v: Vector3) {

    var isInBlock = false

    //public long lastFreeTime = 0;
    //public Location lastFreePos = new Location();

    var lastFloorPos = BlockVector3()
    var lastFreePos = Location()

    var onClimbable = false

    var lastIcePos = Vector3()

    init {
        lastFloorPos.setComponents(v.floorX, v.floorY, v.floorZ)
        lastFreePos.setComponents(v.floorX.toDouble(), v.floorY.toDouble(), v.floorZ.toDouble())
    }
}
