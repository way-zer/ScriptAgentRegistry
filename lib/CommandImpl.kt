@file:Suppress("unused")

package coreBukkit.lib

import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.thisContextScript
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import java.util.logging.Level

class BukkitCommandWrapper(val info: CommandInfo) :
    Command(info.name, info.description.toString(), info.usage, info.aliases),
    CommandExecutor, TabCompleter {
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<out String>): Boolean {
        val content = CommandContext().apply {
            reply = { msg ->
                msg.with("receiver" to sender, "player" to sender).toString()
                    .let {
                        ColorApi.handle(
                            it,
                            ::minecraftColorHandler
                        )
                    }
                    .let(sender::sendMessage)
            }
            this.sender = sender
            hasPermission = { sender.hasPermission(it) }
            prefix = "/$commandLabel "
            arg = args.toList()
        }
        val dispatcher = if (sender is Player) Dispatchers.entity(sender) else Dispatchers.game
        val scope = (info.script ?: thisContextScript().apply {
            logger.log(Level.WARNING, "$info don't associate with Script, it's required for CoroutineScope")
        })
        scope.launch(dispatcher) {
            with(content) { info.handle() }
        }
        return true
    }

    override fun tabComplete(sender: CommandSender, alias: String, args: Array<out String>): List<String> {
        var result: List<String> = emptyList()
        val context = CommandContext().apply {
            this.sender = sender
            hasPermission = { sender.hasPermission(it) }
            replyTabComplete = { result = it;CommandInfo.Return() }
            prefix = "/$alias "
            arg = args.toList()
        }
        if (sender is Player) {
            Dispatchers.entity(sender).safeBlocking {
                try {
                    info.onComplete(context)
                } catch (_: CommandInfo.Return) {
                }
            }
        } else {
            //Console use console thread, not main thread
            runBlocking(Dispatchers.game) {
                try {
                    info.onComplete(context)
                } catch (_: CommandInfo.Return) {
                }
            }
        }
        return result.filter { it.startsWith(args.last()) }//Bukkit don't filter
    }

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        return execute(sender, label, args)
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return tabComplete(sender, alias, args)
    }

    companion object {
        fun registryGlobal(command: CommandInfo) {
            @Suppress("UnstableApiUsage")
            Bukkit.getCommandMap().register(Config.pluginMain.pluginMeta.name, BukkitCommandWrapper(command))
        }

        fun unregisterGlobal(command: CommandInfo) {
            Bukkit.getCommandMap().knownCommands.apply {
                entries.filter {//removeIf not supported
                    (it.value as? BukkitCommandWrapper)?.info == command
                }.forEach {
                    remove(it.key, it.value)
                }
            }
        }

        @Suppress("DEPRECATION")
        fun minecraftColorHandler(color: ColorApi.Color): String {
            return when (color) {
                ConsoleColor.RESET -> org.bukkit.ChatColor.RESET
                ConsoleColor.BOLD -> org.bukkit.ChatColor.BOLD
                ConsoleColor.ITALIC -> org.bukkit.ChatColor.ITALIC
                ConsoleColor.UNDERLINED -> org.bukkit.ChatColor.UNDERLINE
                ConsoleColor.BLACK -> org.bukkit.ChatColor.BLACK
                ConsoleColor.RED -> org.bukkit.ChatColor.DARK_RED
                ConsoleColor.GREEN -> org.bukkit.ChatColor.DARK_GREEN
                ConsoleColor.YELLOW -> org.bukkit.ChatColor.YELLOW
                ConsoleColor.BLUE -> org.bukkit.ChatColor.DARK_BLUE
                ConsoleColor.PURPLE -> org.bukkit.ChatColor.DARK_PURPLE
                ConsoleColor.CYAN -> org.bukkit.ChatColor.DARK_AQUA
                ConsoleColor.LIGHT_RED -> org.bukkit.ChatColor.RED
                ConsoleColor.LIGHT_GREEN -> org.bukkit.ChatColor.GREEN
                ConsoleColor.LIGHT_YELLOW -> org.bukkit.ChatColor.YELLOW
                ConsoleColor.LIGHT_BLUE -> org.bukkit.ChatColor.BLUE
                ConsoleColor.LIGHT_PURPLE -> org.bukkit.ChatColor.LIGHT_PURPLE
                ConsoleColor.LIGHT_CYAN -> org.bukkit.ChatColor.AQUA
                ConsoleColor.WHITE -> org.bukkit.ChatColor.WHITE
                else -> return ""
            }.toString()
        }
    }
}

var CommandContext.sender by DSLBuilder.dataKey<CommandSender>()
val CommandContext.player get() = sender as? Player