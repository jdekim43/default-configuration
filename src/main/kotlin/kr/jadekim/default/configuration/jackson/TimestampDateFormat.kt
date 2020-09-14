package kr.jadekim.default.configuration.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import kr.jadekim.logger.JLog
import java.text.DateFormat
import java.text.FieldPosition
import java.text.ParsePosition
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class TimestampDateFormat : DateFormat() {

    override fun parse(source: String?, pos: ParsePosition?): Date? {
        return source?.toLongOrNull()?.let { Date(it) }
    }

    override fun format(date: Date?, toAppendTo: StringBuffer?, fieldPosition: FieldPosition?): StringBuffer? {
        if (toAppendTo == null || date == null) {
            return null
        }

        return toAppendTo.append(date.time)
    }
}

class DateSerializer : JsonSerializer<Date>() {

    override fun serialize(value: Date?, gen: JsonGenerator, serializers: SerializerProvider) {
        value?.time?.let {
            gen.writeNumber(it)
        }
    }
}

class DateDeserializer : JsonDeserializer<Date>() {

    private val logger = JLog.get(javaClass)

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Date? {
        return try {
            Date(p.longValue)
        } catch (e: Exception) {
            logger.warning("Fail to deserialize Date")
            null
        }
    }
}

class LocalDateTimeSerializer : JsonSerializer<LocalDateTime>() {

    override fun serialize(value: LocalDateTime?, gen: JsonGenerator, serializers: SerializerProvider) {
        value?.atOffset(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()?.let {
            gen.writeNumber(it)
        }
    }
}

class LocalDateTimeDeserializer : JsonDeserializer<LocalDateTime>() {

    private val logger = JLog.get(javaClass)

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): LocalDateTime? {
        return try {
            LocalDateTime.ofEpochSecond(p.longValue / 1000, (p.longValue % 1000).toInt() * 1000000, ZoneOffset.UTC)
        } catch (e: Exception) {
            logger.warning("Fail to deserialize DateTime")
            null
        }
    }
}

val timestampModule = SimpleModule()
        .addSerializer(Date::class.java, DateSerializer())
        .addDeserializer(Date::class.java, DateDeserializer())
        .addSerializer(LocalDateTime::class.java, LocalDateTimeSerializer())
        .addDeserializer(LocalDateTime::class.java, LocalDateTimeDeserializer())!!