package cz.creeperface.nukkit.gac.checks.data

class MotionData {

    var time: Long = -1
    var timeY: Long = 0

    var x: Double = 0.toDouble()
    var y: Double = 0.toDouble()
    var z: Double = 0.toDouble()

    var fromX: Double = 0.toDouble()
    var fromZ: Double = 0.toDouble()

    var distance: Double = 0.toDouble()
    var ground: Boolean = false

    var groundTime: Long = -1

    val isEmpty: Boolean
        get() = this.time == (-1).toLong()

    fun clear() {
        this.time = -1
        this.groundTime = -1
    }
}
