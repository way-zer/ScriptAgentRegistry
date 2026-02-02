@file:Import("io.ktor:ktor-server-call-logging-jvm:3.3.3", mavenDepends = true)

package ktor

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.plugins.contentnegotiation.*
import org.slf4j.event.Level

ktorInit {
    install(ContentNegotiation) {
        json()
    }
    install(CallLogging) {
        level = Level.INFO
    }
}

routing {
    get("/testEnable") {
        call.respond("Powered by ktor and ScriptAgent")
    }
}