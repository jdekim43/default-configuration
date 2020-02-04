package kr.jadekim.default.configuration.jlog

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import kr.jadekim.common.apiserver.enumuration.Environment
import kr.jadekim.default.configuration.jackson.Jackson
import kr.jadekim.logger.JLog
import kr.jadekim.logger.model.Level
import kr.jadekim.logger.printer.JsonPrinter
import kr.jadekim.logger.printer.TextPrinter

private val listSerializeModule = SimpleModule()
    .addSerializer(List::class.java, ListSerializer())

private val mapper = Jackson.copy()
    .setSerializationInclusion(JsonInclude.Include.NON_NULL)
    .setSerializationInclusion(JsonInclude.Include.NON_EMPTY)
    .registerModule(listSerializeModule)!!

fun JLog.default(serviceEnv: Environment, applicationPackages: List<String> = emptyList(), isAsync: Boolean = true) {
    autoClassNamer()

    val printer = if (serviceEnv == Environment.LOCAL) {
        TextPrinter()
    } else {
        JsonPrinter(mapper)
    }

    if (isAsync) {
        addAsyncPrinter(printer)
    } else {
        addPrinter(printer)
    }

    defaultLoggerLevel(serviceEnv, applicationPackages)
}

fun JLog.defaultLoggerLevel(serviceEnv: Environment, applicationPackages: List<String> = emptyList()) {
    prefix("HttpClientLogger", Level.TRACE)

    when (serviceEnv) {
        Environment.PRODUCTION, Environment.STAGE -> {
            defaultLoggerLevel = Level.WARNING
            exactly("ErrorLogger", Level.INFO)

            applicationPackages.forEach { prefix(it, Level.INFO) }
        }
        Environment.QA, Environment.DEVELOPMENT -> {
            defaultLoggerLevel = Level.WARNING
            exactly("ErrorLogger", Level.DEBUG)
            exactly("Exposed", Level.DEBUG)
            prefix("com.zaxxer.hikari", Level.INFO)
            prefix("io.lettuce", Level.INFO)

            applicationPackages.forEach { prefix(it, Level.DEBUG) }
        }
        Environment.LOCAL -> {
            defaultLoggerLevel = Level.INFO
            exactly("ErrorLogger", Level.TRACE)
            exactly("Exposed", Level.DEBUG)
            prefix("org.koin", Level.WARNING)
            prefix("io.sentry", Level.WARNING)

            applicationPackages.forEach { prefix(it, Level.TRACE) }
        }
    }
}

class ListSerializer : JsonSerializer<List<*>>() {

    private val listSerializeMapper = Jackson

    override fun serialize(value: List<*>?, gen: JsonGenerator, serializers: SerializerProvider) {
        value
            ?.let { listSerializeMapper.writeValueAsString(it) }
            ?.let { gen.writeString(it) }
    }
}