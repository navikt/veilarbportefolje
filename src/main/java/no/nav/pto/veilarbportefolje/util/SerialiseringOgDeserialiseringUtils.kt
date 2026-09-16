package no.nav.pto.veilarbportefolje.util

import no.nav.common.json.JsonUtils
import no.nav.common.rest.client.RestUtils
import okhttp3.Response
import tools.jackson.module.kotlin.readValue

val objectMapper = JsonUtils.getMapper()

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
