package cz.creeperface.nukkit.gac.utils

/**
 * @author CreeperFace
 */
data class Configuration(
        val active: Boolean,
        val excludedLevels: Set<String>,
        val kick: Boolean,
        val checkOPs: Boolean,
        val checks: Map<Int, Boolean>,
        val spamDelay: Int,
        val hitRange: Double,
        val enableElytra: Boolean,
        val maxDistance: Double
) {

    fun enabled(checkType: CheckType) = this.checks[checkType.ordinal] ?: false

    companion object {

        const val VERSION = 5
    }
}

enum class CheckType {
    SPEED,
    FLY,
    SPEEDMINE,
    NUKER,
    GLIDE,
    TELEPORT,
    NOCLIP,
    SPAM,
    REACH,
    AIMBOT,
    BHOP
}