package kr.jadekim.default.configuration.ktor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
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
import kr.jadekim.common.apiserver.AbstractServer
import kr.jadekim.common.apiserver.enumuration.Environment
import kr.jadekim.common.apiserver.enumuration.IEnvironment
import kr.jadekim.common.apiserver.exception.ApiException
import kr.jadekim.common.util.exception.ExceptionLevel
import kr.jadekim.common.util.exception.FriendlyException
import kr.jadekim.default.configuration.jackson.Jackson
import kr.jadekim.logger.JLog
import kr.jadekim.logger.integration.KtorLogContextFeature
import kr.jadekim.logger.model.Level
import kr.jadekim.server.ktor.converter.JacksonConverter
import kr.jadekim.server.ktor.feature.PathNormalizeFeature
import kr.jadekim.server.ktor.feature.RequestLogFeature
import kr.jadekim.server.ktor.jsonBodyMapper
import kr.jadekim.server.ktor.locale
import java.time.Duration

abstract class BaseKtorServer(
        serviceEnv: IEnvironment = Environment.LOCAL,
        port: Int = 8080,
        release: String = "not_set",
        private val jackson: ObjectMapper = jacksonObjectMapper(),
        serverName: String? = null
) : AbstractServer(serviceEnv, port, release, serverName) {

    protected open val filterParameters: List<String> = emptyList()

    protected open val server: ApplicationEngine = embeddedServer(Netty, port = port) {
        configure()
    }

    protected val errorLogger = JLog.get("ErrorLogger")

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
                requestLogContext().forEach { (k, v) -> it[k] = v }
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

        install(StatusPages) { configureErrorHandler(this) }
    }

    open fun configureErrorHandler(configuration: StatusPages.Configuration) {
        with(configuration) {
            status(HttpStatusCode.InternalServerError) {
                errorLogger.sLog(Level.ERROR, "InternalServerError-UnknownException")

                responseError(ApiException("UKN-1", 500, logLevel = Level.ERROR))
            }
            exception<Throwable> {
                val wrapper = ApiException("UKN-2", 500, cause = it, message = it.message, logLevel = Level.ERROR)

                errorLogger.sLog(Level.ERROR, wrapper.message ?: it.javaClass.simpleName, wrapper)

                responseError(wrapper)
            }
            exception<FriendlyException> {
                val errorContext = it.data?.let { c -> Jackson.convertValue<Map<String, Any?>>(c) } ?: emptyMap()

                errorLogger.sLog(it.level.logLevel, it.message ?: it.javaClass.simpleName, it, errorContext)

                responseError(
                        ApiException(
                                it.code,
                                it.level.httpCode,
                                cause = it,
                                message = it.message,
                                logLevel = it.level.logLevel,
                                data = it.data
                        )
                )
            }
            exception<ApiException> {
                val errorContext = it.data?.let { c -> Jackson.convertValue<Map<String, Any?>>(c) } ?: emptyMap()

                errorLogger.sLog(it.logLevel, it.message ?: it.javaClass.simpleName, it, errorContext)

                responseError(it)
            }
        }
    }

    open fun Application.installExtraFeature() {

    }

    open fun PipelineContext<Unit, ApplicationCall>.requestLogContext(): Map<String, Any?> = emptyMap()

    fun start(blocking: Boolean = true) {
        logger.info("Start $serverName : service_env=${serviceEnv.name}, service_port=$port")

        server.start(wait = blocking)
    }

    fun stop(gracePeriod: Long = 1, timeout: Duration = Duration.ofSeconds(30)) {
        logger.info("Request stop $serverName")
        server.stop(gracePeriod, timeout.toMillis())
        logger.info("Stopped $serverName")
    }

    override fun start() = start(true)

    override fun stop(timeout: Duration) = stop(1, timeout)

    protected open suspend fun PipelineContext<*, ApplicationCall>.responseError(exception: ApiException) {
        context.respond(HttpStatusCode.fromValue(exception.httpStatus), exception.toResponse(locale))
    }

    private val ExceptionLevel.logLevel
        get() = when (this) {
            ExceptionLevel.ERROR -> Level.ERROR
            ExceptionLevel.WARNING -> Level.WARNING
            else -> Level.INFO
        }

    private val ExceptionLevel.httpCode
        get() = when (this) {
            ExceptionLevel.ERROR -> 500
            else -> 400
        }
}