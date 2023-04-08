@file:Import("io.ktor:ktor-server-call-logging-jvm:2.2.4", mavenDepends = true)

package ktor

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import io.ktor.serialization.jackson.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import org.slf4j.event.Level

ktorInit {
    install(ContentNegotiation) {
        jackson {
            val module = SimpleModule()
            module.addDeserializer(Parameters::class.java, object : JsonDeserializer<Parameters>() {
                override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Parameters {
                    return Parameters.build {
                        while (true)
                            append(p.nextFieldName() ?: break, p.nextTextValue())
                    }
                }
            })
            registerModule(module)
        }
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