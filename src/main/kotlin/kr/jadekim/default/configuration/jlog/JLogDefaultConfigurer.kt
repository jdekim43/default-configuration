package kr.jadekim.default.configuration.jlog

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import kr.jadekim.common.apiserver.enumuration.Environment
import kr.jadekim.common.apiserver.enumuration.IEnvironment
import kr.jadekim.default.configuration.gson.Gson
import kr.jadekim.logger.JLog
import kr.jadekim.logger.model.Level
import kr.jadekim.logger.printer.GsonPrinter
import kr.jadekim.logger.printer.TextPrinter
import java.lang.reflect.Type

private val gson = Gson.newBuilder()
        .registerTypeAdapter(List::class.java, ListSerializer())
        .create()

fun JLog.default(serviceEnv: IEnvironment, applicationPackages: List<String> = emptyList(), isAsync: Boolean = true) {
    autoClassNamer()

    val printer = if (serviceEnv.name == Environment.LOCAL.name) {
        TextPrinter()
    } else {
        GsonPrinter(gson, traceMaxLength = Integer.MAX_VALUE)
    }

    if (isAsync) {
        addAsyncPrinter(printer)
    } else {
        addPrinter(printer)
    }

    defaultLoggerLevel(serviceEnv, applicationPackages)
}

fun JLog.defaultLoggerLevel(serviceEnv: IEnvironment, applicationPackages: List<String> = emptyList()) {
    prefix("HttpClientLogger", Level.TRACE)

    when (serviceEnv.name) {
        Environment.PRODUCTION.name, Environment.STAGE.name -> {
            defaultLoggerLevel = Level.WARNING
            exactly("ErrorLogger", Level.INFO)

            applicationPackages.forEach { prefix(it, Level.INFO) }
        }
        Environment.QA.name, Environment.DEVELOPMENT.name -> {
            defaultLoggerLevel = Level.WARNING
            exactly("ErrorLogger", Level.DEBUG)
            exactly("Exposed", Level.DEBUG)
            prefix("com.zaxxer.hikari", Level.INFO)
            prefix("io.lettuce", Level.INFO)

            applicationPackages.forEach { prefix(it, Level.DEBUG) }
        }
        Environment.LOCAL.name -> {
            defaultLoggerLevel = Level.INFO
            exactly("ErrorLogger", Level.TRACE)
            exactly("Exposed", Level.DEBUG)
            prefix("org.koin", Level.WARNING)
            prefix("io.sentry", Level.WARNING)

            applicationPackages.forEach { prefix(it, Level.TRACE) }
        }
    }
}

class ListSerializer : JsonSerializer<List<*>> {

    override fun serialize(src: List<*>, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return JsonPrimitive(gson.toJson(src))
    }
}