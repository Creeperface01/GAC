package cz.creeperface.nukkit.gac.player.utils

interface ILocation : IPosition {

    var yaw: Double
    var pitch: Double

    val floorX: Double
    val floorY: Double
    val floorZ: Double

    val chunkX: Double
    val chunkZ: Double
}