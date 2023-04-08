@file:Depends("coreLibrary")
@file:Import("io.ktor:ktor-server-jetty-jvm:2.2.4", mavenDepends = true)
@file:Import("ktor.lib.*", defaultImport = true)
@file:Import("io.ktor.http.*", defaultImport = true)
@file:Import("io.ktor.server.application.*", defaultImport = true)
@file:Import("io.ktor.server.routing.*", defaultImport = true)
@file:Import("io.ktor.server.request.*", defaultImport = true)
@file:Import("io.ktor.server.response.*", defaultImport = true)

package ktor

import cf.wayzer.scriptAgent.events.ScriptEnableEvent
import io.ktor.server.engine.*
import io.ktor.server.jetty.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import ktor.lib.util.SimpleApplicationEngineEnvironment

val port by config.key(9090, "Web 端口")
val scriptThis = this


onEnable {
    val env = SimpleApplicationEngineEnvironment {
        parentCoroutineContext = scriptThis.coroutineContext
        connector { port = scriptThis.port }
        module {
            ScriptRegistry.allScripts { it.enabled }.forEach { s ->
                s.inst!!.webInit.forEach { it() }
                s.inst!!.ktorInit.forEach { it() }
            }
        }
    }
    val server = embeddedServer(Jetty, env).start()
    onDisable {
        server.stop(1000, 5000)
    }
    @OptIn(FlowPreview::class)
    reloadFlow.debounce(1000)
        .onEach { env.reload() }
        .launchIn(this)
}

val reloadFlow = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

listenTo<ScriptEnableEvent>(Event.Priority.After) {
    if (script.dslExists(webInit) || script.dslExists(ktorInit))
        reloadFlow.emit(Unit)
}