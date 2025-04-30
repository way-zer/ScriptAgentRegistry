@file:Suppress("unused")

package coreBukkit.lib

import cf.wayzer.scriptAgent.Config
import cf.wayzer.scriptAgent.thisContextScript
import cf.wayzer.scriptAgent.util.DSLBuilder
import coreLibrary.lib.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.ChatColor
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
        val scope = (info.script ?: thisContextScript().apply {
            logger.log(Level.WARNING, "$info don't associate with Script, it's required for CoroutineScope")
        })
        scope.launch(Dispatchers.game) {
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

        BukkitDispatcher.safeBlocking {
            try {
                info.onComplete(context)
            } catch (_: CommandInfo.Return) {
            }
        }
        return result
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
            Bukkit.getCommandMap().register(Config.pluginMain.pluginMeta.name, BukkitCommandWrapper(command))
        }

        fun unregisterGlobal(command: CommandInfo) {
            Bukkit.getCommandMap().knownCommands.entries.removeIf {
                (it.value as? BukkitCommandWrapper)?.info == command
            }
        }

        fun minecraftColorHandler(color: ColorApi.Color): String {
            return when (color) {
                ConsoleColor.RESET -> ChatColor.RESET
                ConsoleColor.BOLD -> ChatColor.BOLD
                ConsoleColor.ITALIC -> ChatColor.ITALIC
                ConsoleColor.UNDERLINED -> ChatColor.UNDERLINE
                ConsoleColor.BLACK -> ChatColor.BLACK
                ConsoleColor.RED -> ChatColor.DARK_RED
                ConsoleColor.GREEN -> ChatColor.DARK_GREEN
                ConsoleColor.YELLOW -> ChatColor.YELLOW
                ConsoleColor.BLUE -> ChatColor.DARK_BLUE
                ConsoleColor.PURPLE -> ChatColor.DARK_PURPLE
                ConsoleColor.CYAN -> ChatColor.DARK_AQUA
                ConsoleColor.LIGHT_RED -> ChatColor.RED
                ConsoleColor.LIGHT_GREEN -> ChatColor.GREEN
                ConsoleColor.LIGHT_YELLOW -> ChatColor.YELLOW
                ConsoleColor.LIGHT_BLUE -> ChatColor.BLUE
                ConsoleColor.LIGHT_PURPLE -> ChatColor.LIGHT_PURPLE
                ConsoleColor.LIGHT_CYAN -> ChatColor.AQUA
                ConsoleColor.WHITE -> ChatColor.WHITE
                else -> return ""
            }.toString()
        }
    }
}

var CommandContext.sender by DSLBuilder.dataKey<CommandSender>()
val CommandContext.player get() = sender as? Player