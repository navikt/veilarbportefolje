package no.nav.pto.veilarbportefolje.util

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.common.rest.client.RestUtils
import okhttp3.Response

// Objectmapperen i common-java-modules har p.t ikke mulighet til å konfigureres. Vi trenger å registrere jackson-kotlin-module for at deserialisering skal fungere med kotlin.
val objectMapper: ObjectMapper =
    no.nav.common.json.JsonUtils.getMapper().registerModule(KotlinModule.Builder().build())

inline fun <reified T> Response.deserializeJson(): T {
    return RestUtils.getBodyStr(this)
        .map {
            // For å støtte at vi kan deserialisere til List<T> og andre typer som ikke er enkle å deserialisere til med T::class.java.
            // Legg merke til at vi importerer readValue extension-funksjonen fra jackson-module-kotlin.
            // Denne funksjonen krever at vi først gjør en assignment, slik at den har not informasjon for
            // å gjøre type inference på hva T er.
            val result: T = objectMapper.readValue(it)
            result
        }
        .orElse(null)
}

inline fun <reified T> Response.deserializeJsonOrThrow(): T {
    return this.deserializeJson()
        ?: throw IllegalStateException("Unable to parse JSON object from response body")
}