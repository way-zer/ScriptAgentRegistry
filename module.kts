@file:Depends("coreLibrary")
@file:CompileDepends("kcp/serialization")

@file:Import("io.ktor:ktor-server-netty-jvm:3.1.1", mavenDepends = true)
@file:Import("io.ktor:ktor-server-content-negotiation-jvm:3.1.1", mavenDepends = true)
@file:Import("io.ktor:ktor-client-content-negotiation-jvm:3.1.1", mavenDepends = true)
@file:Import("io.ktor:ktor-serialization-kotlinx-json-jvm:3.1.1", mavenDepends = true)
@file:Import("io.ktor:ktor-client-cio-jvm:3.1.1", mavenDepends = true)

@file:Import("ktor.lib.*", defaultImport = true)
@file:Import("io.ktor.http.*", defaultImport = true)
@file:Import("io.ktor.server.application.*", defaultImport = true)
@file:Import("io.ktor.server.routing.*", defaultImport = true)
@file:Import("io.ktor.server.request.*", defaultImport = true)
@file:Import("io.ktor.server.response.*", defaultImport = true)

package ktor

import cf.wayzer.scriptAgent.events.ScriptEnableEvent
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.util.logging.Level

val port by config.key(9090, "Web 端口")

onEnable {
    KtorClient = HttpClient(CIO) {
        install(io.ktor.client.plugins.contentnegotiation.ContentNegotiation) {
            json()
        }
    }
    onDisable { KtorClient.close() }
}

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
        .onEach {
            try {
                server.reload()
            } catch (e: Throwable) {
                logger.log(Level.WARNING, "Exception when reload", e)
            }
        }.launchIn(this)
}

val reloadFlow = MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

listenTo<ScriptEnableEvent>(Event.Priority.After) {
    if (script.dslExists(ktorInit))
        reloadFlow.emit(Unit)
}