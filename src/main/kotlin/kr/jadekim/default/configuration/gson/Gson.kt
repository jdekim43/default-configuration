package kr.jadekim.default.configuration.gson

import com.google.gson.GsonBuilder
import kr.jadekim.gson.adapter.KotlinTypeAdapterFactory
import java.time.LocalDateTime
import java.util.Date

val Gson = GsonBuilder()
    .registerTypeAdapter(Date::class.java, DateTypeAdapter())
    .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
    .registerTypeAdapterFactory(KotlinTypeAdapterFactory())
    .create()