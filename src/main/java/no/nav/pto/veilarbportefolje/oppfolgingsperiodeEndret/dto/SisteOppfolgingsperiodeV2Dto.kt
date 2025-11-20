package no.nav.pto.veilarbportefolje.oppfolgingsperiodeEndret.dto

import com.fasterxml.jackson.module.kotlin.treeToValue
import no.nav.pto.veilarbportefolje.util.objectMapper
import org.apache.kafka.common.serialization.Deserializer
import java.time.ZonedDateTime
import java.util.UUID

abstract class SisteOppfolgingsperiodeV2Dto constructor(
    val oppfolgingsperiodeUuid: UUID,
    val startTidspunkt: ZonedDateTime,
    val sisteEndringsType: SisteEndringsType,
    val aktorId: String,
    val ident: String,
    val producerTimestamp: ZonedDateTime,
) {
    companion object {
        fun jsonDeserializer(): Deserializer<SisteOppfolgingsperiodeV2Dto> {
            return object : Deserializer<SisteOppfolgingsperiodeV2Dto> {
                override fun deserialize(topic: String?, data: ByteArray?): SisteOppfolgingsperiodeV2Dto? {
                    if (data == null) return null
                    val jsonTree = objectMapper.readTree(data)
                    val sisteEndringsType = jsonTree.get("sisteEndringsType").asText().let { SisteEndringsType.valueOf(it) }
                    return when (sisteEndringsType) {
                        SisteEndringsType.OPPFOLGING_STARTET, SisteEndringsType.ARBEIDSOPPFOLGINGSKONTOR_ENDRET -> objectMapper.treeToValue<SisteOppfolgingsperiodeV2Dto>(jsonTree)
                        SisteEndringsType.OPPFOLGING_AVSLUTTET -> objectMapper.treeToValue<AvsluttetOppfolgingsperiodeV2>(jsonTree)
                    }
                }
            }
        }
    }
}
