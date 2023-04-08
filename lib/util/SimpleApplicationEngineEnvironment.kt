package ktor.lib.util

import io.ktor.events.*
import io.ktor.server.application.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicReference

class SimpleApplicationEngineEnvironment(build: ApplicationEngineEnvironmentBuilder.() -> Unit) :
    ApplicationEngineEnvironment {
    private val builder = ApplicationEngineEnvironmentBuilder().apply(build)
    override val config: ApplicationConfig = builder.config
    override val developmentMode: Boolean = builder.developmentMode
    override val monitor: Events = Events()
    override val log: Logger = builder.log
    override val parentCoroutineContext = builder.parentCoroutineContext
    override val rootPath: String = builder.rootPath
    override val classLoader: ClassLoader = builder.classLoader
    override val connectors: List<EngineConnectorConfig> = builder.connectors

    //don't know why, but we must new Application as default.
    private val _application = AtomicReference(Application(this))
    override val application: Application
        get() = _application.get() ?: error("Application not start")

    private fun createApplication(): Application {
        val app = Application(this)
        monitor.raise(ApplicationStarting, app)
        builder.modules.forEach { it(app) }
        monitor.raise(ApplicationStarted, app)
        return app
    }

    override fun start() = reload()

    override fun stop() = reload(next = null)

    fun reload(next: Application? = createApplication()) {
        _application.getAndSet(next)?.let {
            monitor.raise(ApplicationStopping, it)
            it.dispose()
            monitor.raise(ApplicationStopped, it)
        }
    }
}