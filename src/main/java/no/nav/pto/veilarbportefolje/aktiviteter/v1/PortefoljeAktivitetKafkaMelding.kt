package no.nav.pto.veilarbportefolje.aktiviteter.v1

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import com.fasterxml.jackson.annotation.Nulls
import no.nav.pto.veilarbportefolje.kafka.KafkaMeldingMedMetadata

@JsonIgnoreProperties(ignoreUnknown = true)
data class PortefoljeAktivitetKafkaMelding(
    @param:JsonProperty("aktivitetId", required = true)
    @param:JsonSetter(nulls = Nulls.FAIL)
    val aktivitetId: String,
    @param:JsonProperty("version", required = true)
    @param:JsonSetter(nulls = Nulls.FAIL)
    val version: Long,
    @param:JsonProperty("aktorId", required = true)
    @param:JsonSetter(nulls = Nulls.FAIL)
    val aktorId: String,
    @param:JsonProperty("fraDato")
    val fraDato: String? = null,
    @param:JsonProperty("tilDato")
    val tilDato: String? = null,
    @param:JsonProperty("endretDato")
    val endretDato: String? = null,
    @param:JsonProperty("aktivitetType", required = true)
    @param:JsonSetter(nulls = Nulls.FAIL)
    val aktivitetType: String,
    @param:JsonProperty("aktivitetStatus", required = true)
    @param:JsonSetter(nulls = Nulls.FAIL)
    val aktivitetStatus: String,
    @param:JsonProperty("endringsType", required = true)
    @param:JsonSetter(nulls = Nulls.FAIL)
    val endringsType: String,
    @param:JsonProperty("lagtInnAv", required = true)
    val lagtInnAv: String?,
    @param:JsonProperty("stillingFraNavData")
    val stillingFraNavData: StillingFraNavPortefoljeData? = null,
    @param:JsonProperty("avtalt", required = true)
    @param:JsonSetter(nulls = Nulls.FAIL)
    val avtalt: Boolean,
    @param:JsonProperty("historisk", required = true)
    @param:JsonSetter(nulls = Nulls.FAIL)
    val historisk: Boolean,
    @param:JsonProperty("tiltakskode")
    val tiltakskode: String? = null,
) {
    fun toEntity(metadata: KafkaMeldingMetadata): KafkaAktivitetMeldingEntity {
        return KafkaAktivitetMeldingEntity(
            aktivitetId = aktivitetId,
            aktorId = aktorId,
            aktivitetType = aktivitetType,
            aktivitetStatus = aktivitetStatus,
            endringsType = endringsType,
            fraDato = fraDato,
            tilDato = tilDato,
            endretDato = endretDato,
            tiltakskode = tiltakskode,
            lagtInnAv = lagtInnAv,
            avtalt = avtalt,
            version = version,
            historisk = historisk,
            cvKanDelesStatus = stillingFraNavData?.cvKanDelesStatus,
            svarfristStillingFraNav = stillingFraNavData?.svarfrist,
            recordOffset = metadata.recordOffset,
            recordPartition = metadata.recordPartition,
            recordKey = metadata.recordKey,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class StillingFraNavPortefoljeData(
        @param:JsonProperty("cvKanDelesStatus")
        val cvKanDelesStatus: String? = null,
        @param:JsonProperty("svarfrist")
        val svarfrist: String? = null,
    )
}

data class KafkaAktivitetMeldingEntity(
    // Aktivitetdata frå Kafka-meldinga
    val aktivitetId: String,
    val aktorId: String,
    val aktivitetType: String,
    val aktivitetStatus: String,
    val endringsType: String,
    val fraDato: String?,
    val tilDato: String?,
    val endretDato: String?,
    val tiltakskode: String?,
    val lagtInnAv: String?,
    val avtalt: Boolean,
    val version: Long,
    val historisk: Boolean,

    // Stilling fra Nav-data frå nested Kafka-payload
    val cvKanDelesStatus: String?,
    val svarfristStillingFraNav: String?,

    // Kafka-metadata
    val recordOffset: Long,
    val recordPartition: Int,
    val recordKey: String,
)

data class KafkaMeldingMetadata(
    val recordOffset: Long,
    val recordPartition: Int,
    val recordKey: String,
)

fun KafkaMeldingMedMetadata<PortefoljeAktivitetKafkaMelding>.toEntity(): KafkaAktivitetMeldingEntity =
    value.toEntity(
        metadata = KafkaMeldingMetadata(
            recordOffset = metadata.offset,
            recordPartition = metadata.partition,
            recordKey = requireNotNull(metadata.key) {
                "Kafka-record mangler key for aktivitet-melding"
            },
        ),
    )
