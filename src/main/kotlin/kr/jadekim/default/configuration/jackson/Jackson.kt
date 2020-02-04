package kr.jadekim.default.configuration.jackson

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

val Jackson = jacksonObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .setDateFormat(TimestampDateFormat())
    .registerModule(timestampModule)!!

val SnakeJackson = Jackson.copy()
    .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)!!