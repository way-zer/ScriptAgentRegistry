@file:Depends("coreLibrary")
@file:Import("io.ktor:ktor-server-netty-jvm:3.0.1", mavenDepends = true)
@file:Import("io.ktor:ktor-server-content-negotiation-jvm:3.0.1", mavenDepends = true)
@file:Import("io.ktor:ktor-serialization-jackson-jvm:3.0.1", mavenDepends = true)
@file:Import("ktor.lib.*", defaultImport = true)
@file:Import("io.ktor.http.*", defaultImport = true)
@file:Import("io.ktor.server.application.*", defaultImport = true)
@file:Import("io.ktor.server.routing.*", defaultImport = true)
@file:Import("io.ktor.server.request.*", defaultImport = true)
@file:Import("io.ktor.server.response.*", defaultImport = true)

package ktor

import cf.wayzer.scriptAgent.events.ScriptEnableEvent
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

val port by config.key(9090, "Web 端口")

onEnable {
    val env = serverConfig {
        parentCoroutineContext = coroutineContext
        module {
            ScriptRegistry.allScripts { it.enabled }.forEach { s ->
                s.inst!!.ktorInit.forEach { it() }
            }
        }
    }
    val port = port
    val server = embeddedServer(Netty, env) {
        connector { this.port = port }
    }.start()
    onDisable {
        server.stop(1000, 5000)
    }
    @OptIn(FlowPreview::class)
    reloadFlow.debounce(1000)
        .onEach { server.reload() }
        .launchIn(this)
}

val reloadFlow = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

listenTo<ScriptEnableEvent>(Event.Priority.After) {
    if (script.dslExists(ktorInit))
        reloadFlow.emit(Unit)
}