package com.ribbontek.spaceinvaders.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.registerKotlinModule

val objectMapper: ObjectMapper = createObjectMapper()

fun <T> T.toJson(): String = objectMapper.writeValueAsString(this)

fun <T> String.fromJson(clazz: Class<T>): T = objectMapper.readValue(this, clazz)

fun createObjectMapper(): ObjectMapper = with(ObjectMapper()) {
    registerKotlinModule()
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    setSerializationInclusion(JsonInclude.Include.NON_NULL)
    enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
}
