package coreStandalone.lib

import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import coreLibrary.lib.*

object RootCommands {
    fun trimInput(text: String) = buildString {
        var start = 0
        var end = text.length - 1
        while (start < text.length && text[start] == ' ') start++
        while (end >= 0 && text[end] == ' ') end--
        var lastBlank = false
        for (i in start..end) {
            val nowBlank = text[i] == ' '
            if (!lastBlank || !nowBlank)
                append(text[i])
            lastBlank = nowBlank
        }
    }

    /**
     * @param text 输入字符串，应当经过trimInput处理
     */
    suspend fun handleInput(text: String, prefix: String = "") {
        if (text.isEmpty()) return
        CommandContext.Command().apply {
            reply = { println(ColorApi.handle(it.toString(), ColorApi::consoleColorHandler)) }
            this.prefix = prefix
            this.arg = text.split(' ')
            Commands.Root.handle()
        }
    }
}

@ScriptDsl
@Deprecated(
    "move to coreLibrary",
    ReplaceWith("command(name,description.with()){init()}", "coreLibrary.lib.command"),
    DeprecationLevel.HIDDEN
)
fun Script.command(
    name: String,
    description: String,
    init: CommandInfo.() -> Unit = {}
) {
    command(name, description.with()) { init() }
}