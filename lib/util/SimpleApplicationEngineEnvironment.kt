package ktor.lib.util

import io.ktor.application.*
import io.ktor.config.*
import io.ktor.server.engine.*
import org.slf4j.Logger
import java.util.concurrent.atomic.AtomicReference

class SimpleApplicationEngineEnvironment(
    builder: ApplicationEngineEnvironmentBuilder,
    private val initApplication: Application.() -> Unit
) :
    ApplicationEngineEnvironment {
    override val config: ApplicationConfig = builder.config
    override val developmentMode: Boolean = builder.developmentMode
    override val monitor: ApplicationEvents = ApplicationEvents()
    override val classLoader: ClassLoader = builder.classLoader
    override val log: Logger = builder.log
    override val parentCoroutineContext = builder.parentCoroutineContext
    override val rootPath: String = builder.rootPath
    override val connectors: List<EngineConnectorConfig> = builder.connectors
    private val _application = AtomicReference<Application>()
    override val application: Application
        get() = _application.get() ?: error("Application not start")

    private fun createApplication(): Application {
        val app = Application(this)
        monitor.raise(ApplicationStarting, app)
        initApplication(app)
        monitor.raise(ApplicationStarted, app)
        return app
    }

    override fun start() {
        createApplication()
            .let(_application::set)
    }

    override fun stop() {
        _application.getAndSet(null)?.dispose()
    }

    fun reload() {
        createApplication()
            .let(_application::getAndSet)
            ?.dispose()
    }
}