package coreBukkit.lib

import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.thisScript
import cf.wayzer.scriptAgent.util.DSLBuilder
import kotlinx.coroutines.launch
import org.bukkit.command.PluginCommand
import org.bukkit.plugin.java.JavaPlugin

val Config.pluginMain by DSLBuilder.lateInit<JavaPlugin>()
val Config.pluginCommand by DSLBuilder.lateInit<PluginCommand>()
private val Config.delayEnable by DSLBuilder.lateInit<MutableList<Runnable>>()

/** onEnable执行
 * 所有脚本会在插件[JavaPlugin.onLoad]阶段加载完毕，此时只能注册事件，不允许去调用其他插件。
 * 故提供该接口，将延迟到[JavaPlugin.onEnable]阶段执行。重载脚本时，必然已经完成，等效于[Script.onEnable]*/
@ScriptDsl
fun Script.afterPluginsLoaded(block: suspend () -> Unit) = onEnable {
    if (Config.pluginMain.isEnabled) return@onEnable block()
    Config.delayEnable.add {
        if (!enabled) return@add
        thisScript.launch { block() }
    }
}