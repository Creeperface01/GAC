package cz.creeperface.nukkit.gac.utils

import cn.nukkit.event.Event
import cn.nukkit.event.EventPriority
import cn.nukkit.event.Listener
import cn.nukkit.plugin.Plugin

/**
 * @author CreeperFace
 */

fun <T : Event> registerEvent(listener: Listener, plugin: Plugin, clazz: Class<T>, executor: (T) -> Unit, ignoreCancelled: Boolean = false, priority: EventPriority = EventPriority.NORMAL) {
    plugin.server.pluginManager.registerEvent(clazz, listener, priority, { _: Listener?, event: Event? -> executor(event as T) }, plugin, ignoreCancelled)
}