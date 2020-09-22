package kr.jadekim.default.configuration.gson

import com.google.gson.GsonBuilder
import kr.jadekim.gson.adapter.KotlinTypeAdapterFactory
import kr.jadekim.gson.adapter.UUIDTypeAdapter
import kr.jadekim.gson.annotation.ExcludeFieldDeserializeStrategy
import kr.jadekim.gson.annotation.ExcludeFieldSerializeStrategy
import java.time.LocalDateTime
import java.util.Date
import java.util.UUID

val Gson = GsonBuilder()
    .registerTypeAdapter(Date::class.java, DateTypeAdapter())
    .registerTypeAdapter(LocalDateTime::class.java, LocalDateTimeTypeAdapter())
    .registerTypeAdapter(UUID::class.java, UUIDTypeAdapter())
    .registerTypeAdapterFactory(KotlinTypeAdapterFactory())
    .addSerializationExclusionStrategy(ExcludeFieldSerializeStrategy())
    .addDeserializationExclusionStrategy(ExcludeFieldDeserializeStrategy())
    .create()