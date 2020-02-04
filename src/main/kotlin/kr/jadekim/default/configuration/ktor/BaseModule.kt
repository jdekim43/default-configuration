package kr.jadekim.default.configuration.ktor

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.convertValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.AutoHeadResponse
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.request.acceptLanguage
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import kr.jadekim.common.apiserver.enumuration.Environment
import kr.jadekim.common.apiserver.exception.ApiException
import kr.jadekim.common.apiserver.exception.UnknownException
import kr.jadekim.logger.JLog
import kr.jadekim.logger.integration.KtorLogContextFeature
import kr.jadekim.logger.model.Level
import kr.jadekim.server.ktor.converter.JacksonConverter
import kr.jadekim.server.ktor.feature.PathNormalizeFeature
import kr.jadekim.server.ktor.feature.RequestLogFeature
import kr.jadekim.server.ktor.jsonBodyMapper
import java.util.*

fun Application.baseModule(
    serviceEnv: Environment,
    version: String,
    filterParameters: List<String> = emptyList(),
    jackson: ObjectMapper = jacksonObjectMapper(),
    logContext: PipelineContext<Unit, ApplicationCall>.() -> Map<String, String> = { emptyMap() }
) {

    val errorLogger = JLog.get("ErrorLogger")

    install(KtorLogContextFeature)

    install(XForwardedHeaderSupport)

    install(PathNormalizeFeature)

    install(RequestLogFeature) {
        this.serviceEnv = serviceEnv
        this.release = version
        this.filterParameters = filterParameters
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

    install(StatusPages) {
        status(HttpStatusCode.InternalServerError) {
            val acceptLanguage = this.context.request.acceptLanguage()
            val locale = if (acceptLanguage == null || acceptLanguage.contains("KR", ignoreCase = true)) {
                Locale.KOREA
            } else {
                Locale.ENGLISH
            }

            call.respond(HttpStatusCode.InternalServerError, UnknownException(Exception()).toResponse(locale))
        }
        exception<Throwable> {
            val acceptLanguage = this.context.request.acceptLanguage()
            val locale = if (acceptLanguage == null || acceptLanguage.contains("KR", ignoreCase = true)) {
                Locale.KOREA
            } else {
                Locale.ENGLISH
            }

            val wrapper = UnknownException(it, it.message)

            errorLogger.sLog(Level.ERROR, wrapper.message ?: it.javaClass.simpleName, wrapper)

            call.respond(HttpStatusCode.fromValue(wrapper.httpStatus), wrapper.toResponse(locale))
        }
        exception<ApiException> {
            val acceptLanguage = this.context.request.acceptLanguage()
            val locale = if (acceptLanguage == null || acceptLanguage.contains("KR", ignoreCase = true)) {
                Locale.KOREA
            } else {
                Locale.ENGLISH
            }

            val errorContext = jackson.convertValue<Map<String, Any?>>(it)
                .filterKeys { it == "cause" }

            errorLogger.sLog(it.logLevel, it.message ?: it.javaClass.simpleName, it, errorContext)

            call.respond(HttpStatusCode.fromValue(it.httpStatus), it.toResponse(locale))
        }
    }
}