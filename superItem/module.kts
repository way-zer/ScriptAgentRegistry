//@file:Depends("coreBukkit")
@file:Import("superitem.lib.*", defaultImport = true)
@file:Import("superitem.lib.features.*", defaultImport = true)
@file:Import("org.bukkit.Material", defaultImport = true)
@file:Import("de.tr7zw.nbtapi.NBTItem", libraryByClass = true)

package superitem

import cf.wayzer.scriptAgent.events.ScriptDisableEvent
import cf.wayzer.scriptAgent.events.ScriptEnableEvent
import kotlin.collections.set

name = "SuperItem 模块"

onEnable {
    ConfigManager.init(Config.dataDir.resolve("superitem"))
}

listenTo<ScriptEnableEvent>(Event.Priority.Before) {
    if (!script.isItem) return@listenTo
    val item = script
    item.require(Permission())
    item.features.values.flatten().apply {
        forEach { ConfigManager.loadForFeature(item, it) }
        forEach {
            if (it is Feature.OnPostLoad) {
                it.onPostLoad()
            }
        }
    }

}
listenTo<ScriptEnableEvent>(Event.Priority.After) {
    if (!script.isItem) return@listenTo
    SIManager.items[script.itemName] = script
    ConfigManager.saveForItem(script)
}

listenTo<ScriptDisableEvent>(Event.Priority.After) {
    if (!script.isItem) return@listenTo
    SIManager.items.remove(script.itemName)
}