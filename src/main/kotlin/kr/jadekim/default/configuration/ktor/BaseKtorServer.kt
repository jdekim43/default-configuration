package kr.jadekim.default.configuration.ktor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.routing.Routing
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext
import kr.jadekim.common.apiserver.enumuration.Environment
import kr.jadekim.logger.JLog
import kr.jadekim.logger.context.GlobalLogContext
import java.util.concurrent.TimeUnit

abstract class BaseKtorServer(
    val serviceEnv: Environment = Environment.LOCAL,
    val port: Int = 8080,
    val release: String = "not_set",
    private val jackson: ObjectMapper = jacksonObjectMapper(),
    serverName: String? = null
) {

    val serverName = serverName ?: javaClass.simpleName!!

    protected open val filterParameters: List<String> = emptyList()

    protected open val server: ApplicationEngine = embeddedServer(Netty, port = port) {
        configure()
    }

    private val logger = JLog.get(javaClass)

    init {
        GlobalLogContext["serviceEnv"] = serviceEnv.name
        GlobalLogContext["servicePort"] = port
        GlobalLogContext["deployVersion"] = release
        GlobalLogContext["serverName"] = this@BaseKtorServer.serverName
    }

    abstract fun Routing.configureRoute()

    fun Application.configure() {
        baseModule(serviceEnv, release, filterParameters, jackson) { logContext() }
        installExtraFeature()

        routing {
            configureRoute()
        }
    }

    open fun Application.installExtraFeature() {

    }

    open fun PipelineContext<Unit, ApplicationCall>.logContext(): Map<String, String> = emptyMap()

    fun start(blocking: Boolean = true) {
        logger.info("Start $serverName : service_env=${serviceEnv.name}, service_port=$port")

        server.start(wait = blocking)
    }

    fun stop() {
        logger.info("Request stop $serverName")
        server.stop(1, 30, TimeUnit.SECONDS)
        logger.info("Stopped $serverName")
    }
}