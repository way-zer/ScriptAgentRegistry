package ktor.lib

import cf.wayzer.scriptAgent.define.Script
import cf.wayzer.scriptAgent.define.ScriptDsl
import cf.wayzer.scriptAgent.util.DSLBuilder
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.routing.*

val Script.ktorInit by DSLBuilder.callbackKey<Application.() -> Unit>()

@ScriptDsl
fun Script.routing(body: Routing.() -> Unit) {
    ktorInit {
        routing(body)
    }
}

//

@Deprecated("use ktorInit", ReplaceWith("this.ktorInit"))
val Script.webInit by DSLBuilder.callbackKey<Application.() -> Unit>()

@Deprecated(
    "use routing", ReplaceWith(
        "routing { if (method == null) route(path, body) else route(path, method, body) }",
        "io.ktor.routing.route",
        "io.ktor.routing.route"
    )
)
fun Script.route(path: String, method: HttpMethod? = null, body: Route.() -> Unit) {
    routing {
        if (method == null)
            route(path, body)
        else
            route(path, method, body)
    }
}