package cz.creeperface.nukkit.gac.player.utils

import cn.nukkit.level.Level

interface IPosition : IVector3 {

    var level: Level
}