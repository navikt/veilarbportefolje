package no.nav.pto.veilarbportefolje.kafka.deserializers

import no.nav.common.json.JsonUtils
import org.apache.kafka.common.serialization.Deserializer

class KotlinJsonDeserializer<T>(private val klasse: Class<T>) : Deserializer<T> {
    val objectMapper = JsonUtils.getMapper()

    override fun deserialize(topic: String?, data: ByteArray?): T? {
        if (data == null) return null
        return objectMapper.readValue(data, klasse)
    }
}
