package kr.jadekim.default.configuration.ktor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.install
import io.ktor.features.AutoHeadResponse
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext
import kr.jadekim.common.apiserver.enumuration.Environment
import kr.jadekim.common.apiserver.enumuration.IEnvironment
import kr.jadekim.common.apiserver.exception.ApiException
import kr.jadekim.common.apiserver.exception.UnknownException
import kr.jadekim.logger.JLog
import kr.jadekim.logger.context.GlobalLogContext
import kr.jadekim.logger.integration.KtorLogContextFeature
import kr.jadekim.logger.model.Level
import kr.jadekim.server.ktor.converter.JacksonConverter
import kr.jadekim.server.ktor.feature.PathNormalizeFeature
import kr.jadekim.server.ktor.feature.RequestLogFeature
import kr.jadekim.server.ktor.jsonBodyMapper
import kr.jadekim.server.ktor.locale
import java.time.Duration

abstract class BaseKtorServer(
        val serviceEnv: IEnvironment = Environment.LOCAL,
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

    protected val errorLogger = JLog.get("ErrorLogger")

    private val logger = JLog.get(javaClass)

    init {
        GlobalLogContext["serviceEnv"] = serviceEnv.name
        GlobalLogContext["servicePort"] = port
        GlobalLogContext["deployVersion"] = release
        GlobalLogContext["serverName"] = this@BaseKtorServer.serverName
    }

    abstract fun Routing.configureRoute()

    fun Application.configure() {
        installFeature()
        installExtraFeature()

        routing {
            configureRoute()
        }
    }

    open fun Application.installFeature() {
        install(KtorLogContextFeature)

        install(XForwardedHeaderSupport)

        install(PathNormalizeFeature)

        install(RequestLogFeature) {
            this.serviceEnv = this@BaseKtorServer.serviceEnv.name
            this.release = this@BaseKtorServer.release
            this.filterParameters = this@BaseKtorServer.filterParameters
            this.logContext = {
                logContext().forEach { (k, v) -> it[k] = v }
            }
        }

        install(AutoHeadResponse)

        jsonBodyMapper = jackson

        install(ContentNegotiation) {
            register(
                    ContentType.Application.Json,
                    JacksonConverter(jackson)
            )
        }

        install(StatusPages) { configureErrorHandler() }
    }

    open fun StatusPages.Configuration.configureErrorHandler() {
        status(HttpStatusCode.InternalServerError) {
            errorLogger.sLog(Level.ERROR, "InternalServerError-UnknownException")

            responseError(UnknownException(Exception()))
        }
        exception<Throwable> {
            val wrapper = UnknownException(it, it.message)

            errorLogger.sLog(Level.ERROR, wrapper.message ?: it.javaClass.simpleName, wrapper)

            responseError(wrapper)
        }
        exception<ApiException> {
            val errorContext = jackson.convertValue<Map<String, Any?>>(it)
                    .filterKeys { it == "cause" }

            errorLogger.sLog(it.logLevel, it.message ?: it.javaClass.simpleName, it, errorContext)

            responseError(it)
        }
    }

    open fun Application.installExtraFeature() {

    }

    open fun PipelineContext<Unit, ApplicationCall>.logContext(): Map<String, Any?> = emptyMap()

    fun start(blocking: Boolean = true) {
        logger.info("Start $serverName : service_env=${serviceEnv.name}, service_port=$port")

        server.start(wait = blocking)
    }

    fun stop(gracePeriod: Long = 1, timeout: Duration = Duration.ofSeconds(30)) {
        logger.info("Request stop $serverName")
        server.stop(gracePeriod, timeout.toMillis())
        logger.info("Stopped $serverName")
    }

    protected open suspend fun PipelineContext<*, ApplicationCall>.responseError(exception: ApiException) {
        context.respond(HttpStatusCode.fromValue(exception.httpStatus), exception.toResponse(locale))
    }
}