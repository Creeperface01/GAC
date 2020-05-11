package cz.creeperface.nukkit.gac.utils

import co.aikar.timings.TimingsManager

object GACTimings {

    private const val prefix = "GAC-"

    val flyCheck = TimingsManager.getTiming("${prefix}flyCheck")
    val speedCheck = TimingsManager.getTiming("${prefix}speedCheck")
    val nukerCheck = TimingsManager.getTiming("${prefix}nukerCheck")
    val collisionCheck = TimingsManager.getTiming("${prefix}collisionCheck")
    val cheatTask = TimingsManager.getTiming("${prefix}cheatTask")
}