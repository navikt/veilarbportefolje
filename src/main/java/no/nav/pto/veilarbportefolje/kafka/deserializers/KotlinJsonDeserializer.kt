package no.nav.pto.veilarbportefolje.kafka.deserializers

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.common.serialization.Deserializer

class KotlinJsonDeserializer<T>(private val klasse: Class<T>) : Deserializer<T> {
    private val objectMapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    override fun deserialize(topic: String?, data: ByteArray?): T? {
        if (data == null) return null
        return objectMapper.readValue(data, klasse)
    }
}
