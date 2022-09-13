@file:Depends("coreLibrary")
@file:Import("io.ktor:ktor-server-jetty:1.6.2", mavenDepends = true)
@file:Import("com.fasterxml.jackson.core:jackson-databind:2.12.3", mavenDependsSingle = true)
@file:Import("com.fasterxml.jackson.core:jackson-core:2.12.3", mavenDependsSingle = true)
@file:Import("com.fasterxml.jackson.core:jackson-annotations:2.12.3", mavenDependsSingle = true)
@file:Import("io.ktor:ktor-jackson:1.6.2", mavenDepends = true)
@file:Import("javax.servlet:javax.servlet-api:3.1.0", mavenDependsSingle = true)
@file:Import("ktor.lib.*", defaultImport = true)
@file:Import("io.ktor.application.*", defaultImport = true)
@file:Import("io.ktor.http.*", defaultImport = true)
@file:Import("io.ktor.routing.*", defaultImport = true)
@file:Import("io.ktor.request.*", defaultImport = true)
@file:Import("io.ktor.response.*", defaultImport = true)

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
    val envBuilder = ApplicationEngineEnvironmentBuilder().apply {
        parentCoroutineContext = scriptThis.coroutineContext
        connector { port = scriptThis.port }
    }
    val env = SimpleApplicationEngineEnvironment(envBuilder) {
        ScriptManager.allScripts { it.enabled }.forEach { s ->
            s.inst!!.webInit.forEach { it() }
            s.inst!!.ktorInit.forEach { it() }
        }
    }
    val server = embeddedServer(Jetty, env)
    server.start()
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