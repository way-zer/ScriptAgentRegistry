package ktor

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import io.ktor.features.*
import io.ktor.jackson.*
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
    routing {
        get("/testEnable") {
            call.respond("Powered by ktor and ScriptAgent")
        }
    }
}