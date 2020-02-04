package kr.jadekim.default.configuration.jackson

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import kr.jadekim.logger.JLog
import org.joda.time.DateTime
import java.text.DateFormat
import java.text.FieldPosition
import java.text.ParsePosition
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

class DateTimeSerializer : JsonSerializer<DateTime>() {

    override fun serialize(value: DateTime?, gen: JsonGenerator, serializers: SerializerProvider) {
        value?.toDate()?.time?.let {
            gen.writeNumber(it)
        }
    }
}

class DateTimeDeserializer : JsonDeserializer<DateTime>() {

    private val logger = JLog.get(javaClass)

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): DateTime? {
        return try {
            DateTime(p.longValue)
        } catch (e: Exception) {
            logger.warning("Fail to deserialize DateTime")
            null
        }
    }
}

val timestampModule = SimpleModule()
    .addSerializer(Date::class.java, DateSerializer())
    .addDeserializer(Date::class.java, DateDeserializer())
    .addSerializer(DateTime::class.java, DateTimeSerializer())
    .addDeserializer(DateTime::class.java, DateTimeDeserializer())!!