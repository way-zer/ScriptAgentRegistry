package ktor.lib

import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.util.DSLBuilder
import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.routing.*

lateinit var KtorClient: HttpClient
val Script.ktorInit by DSLBuilder.callbackKey<Application.() -> Unit>()

@ScriptDsl
fun Script.routing(body: Routing.() -> Unit) {
    ktorInit {
        routing(body)
    }
}

@Deprecated(
    "use routing", ReplaceWith(
        "routing { route(path, method, body) }"
    )
)
fun Script.route(path: String, method: HttpMethod, body: Route.() -> Unit) {
    routing {
        route(path, method, body)
    }
}

@Deprecated(
    "use routing", ReplaceWith(
        "routing { route(path, body) }"
    )
)
fun Script.route(path: String, body: Route.() -> Unit) {
    routing {
        route(path, body)
    }
}