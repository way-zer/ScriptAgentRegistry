@file:Depends("coreLibrary")
@file:Import("org.bukkit.Bukkit", libraryByClassLoader = true)
@file:Import("coreBukkit.lib.*", defaultImport = true)
@file:Import("org.bukkit.ChatColor.*", defaultImport = true)

package coreBukkit

name = "Bukkit 核心脚本模块"

afterPluginsLoaded {
    Commands.Root.addWatcher(thisScript, object : Commands.CommandsWatcher {
        override fun onAdd(command: CommandInfo) {
            if (command.name == "help") return
            if (command.name == "ScriptAgent") {
                val cmd = BukkitCommandWrapper(command)
                Config.pluginCommand.apply {
                    setExecutor(cmd);tabCompleter = cmd
                }
                return
            }
            BukkitCommandWrapper.registryGlobal(command)
        }

        override fun onRemove(command: CommandInfo) {
            BukkitCommandWrapper.unregisterGlobal(command)
        }
    })
}
ListenExt//ensure init