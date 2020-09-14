package kr.jadekim.default.configuration.gson

import com.google.gson.GsonBuilder
import java.time.LocalDateTime
import java.util.*

val Gson = GsonBuilder()
        .registerTypeAdapter(Date::class.java, DateTypeAdapter())
        .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
        .create()