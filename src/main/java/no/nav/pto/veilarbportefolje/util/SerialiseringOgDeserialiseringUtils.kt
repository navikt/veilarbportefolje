package no.nav.pto.veilarbportefolje.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.common.rest.client.RestUtils
import okhttp3.Response

val objectMapper: ObjectMapper = ObjectMapper()
    .findAndRegisterModules()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)
    .configure(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE, false)

inline fun <reified T> Response.deserializeJson(): T? {
    return RestUtils.getBodyStr(this)
        .map {
            val result: T = objectMapper.readValue(it)
            result
        }
        .orElse(null)
}

inline fun <reified T> Response.deserializeJsonOrThrow(): T {
    return this.deserializeJson()
        ?: throw IllegalStateException("Unable to parse JSON object from response body")
}
