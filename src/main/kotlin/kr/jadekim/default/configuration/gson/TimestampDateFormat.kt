package kr.jadekim.default.configuration.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class DateTypeAdapter : TypeAdapter<Date>() {

    override fun write(out: JsonWriter, value: Date?) {
        out.value(value?.time)
    }

    override fun read(input: JsonReader): Date = Date(input.nextLong())
}

class LocalDateTimeTypeAdapter : TypeAdapter<LocalDateTime>() {

    override fun write(out: JsonWriter, value: LocalDateTime?) {
        out.value(value?.atOffset(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
    }

    override fun read(input: JsonReader): LocalDateTime {
        val value = input.nextLong()

        return LocalDateTime.ofEpochSecond(
                value / 1000,
                (value % 1000).toInt() * 1000000,
                ZoneOffset.UTC
        )
    }
}