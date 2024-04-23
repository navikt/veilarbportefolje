package no.nav.pto.veilarbportefolje.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import no.nav.common.rest.client.RestUtils
import okhttp3.Response

// Objectmapperen i common-java-modules har p.t ikke mulighet til å konfigureres. Vi trenger å registrere jackson-kotlin-module for at deserialisering skal fungere med kotlin.
val objectMapper: ObjectMapper =
    no.nav.common.json.JsonUtils.getMapper().registerModule(KotlinModule.Builder().build())

inline fun <reified T> Response.deserializeJson(): T? {
    return RestUtils.getBodyStr(this)
        .map { objectMapper.readValue(it, T::class.java) }
        .orElse(null)
}

inline fun <reified T> Response.deserializeJsonOrThrow(): T {
    return this.deserializeJson()
        ?: throw IllegalStateException("Unable to parse JSON object from response body")
}