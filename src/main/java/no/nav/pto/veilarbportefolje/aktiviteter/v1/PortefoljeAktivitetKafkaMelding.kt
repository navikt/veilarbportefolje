package no.nav.pto.veilarbportefolje.aktiviteter.v1

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.pto.veilarbportefolje.kafka.KafkaMeldingMedMetadata
import no.nav.pto.veilarbportefolje.util.DateUtils
import java.sql.Timestamp
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class PortefoljeAktivitetKafkaMelding(
    @param:JsonProperty("aktivitetId")
    val aktivitetId: String? = null,
    @param:JsonProperty("version")
    val version: Long? = null,
    @param:JsonProperty("aktorId")
    val aktorId: String? = null,
    @param:JsonProperty("fraDato")
    val fraDato: Date? = null,
    @param:JsonProperty("tilDato")
    val tilDato: Date? = null,
    @param:JsonProperty("endretDato")
    val endretDato: Date? = null,
    @param:JsonProperty("aktivitetType")
    val aktivitetType: AktivitetTypeDTO? = null,
    @param:JsonProperty("aktivitetStatus")
    val aktivitetStatus: AktivitetStatus? = null,
    @param:JsonProperty("endringsType")
    val endringsType: EndringsType? = null,
    @param:JsonProperty("lagtInnAv")
    val lagtInnAv: Innsender? = null,
    @param:JsonProperty("stillingFraNavData")
    val stillingFraNavData: StillingFraNavPortefoljeData? = null,
    @param:JsonProperty("avtalt")
    val avtalt: Boolean = false,
    @param:JsonProperty("historisk")
    val historisk: Boolean = false,
    @param:JsonProperty("tiltakskode")
    val tiltakskode: String? = null,
) {
    fun toEntity(metadata: KafkaMeldingMetadata): KafkaAktivitetMeldingEntity {
        return KafkaAktivitetMeldingEntity(
            value = KafkaAktivitetMeldingValue(
                aktivitetId = requireNotNull(aktivitetId) { "Mangler aktivitetId i aktivitet-melding" },
                aktorId = requireNotNull(aktorId) { "Mangler aktorId i aktivitet-melding" },
                aktivitetType = requireNotNull(aktivitetType) { "Mangler aktivitetType i aktivitet-melding" }.name,
                aktivitetStatus = requireNotNull(aktivitetStatus) { "Mangler aktivitetStatus i aktivitet-melding" }.name,
                endringsType = requireNotNull(endringsType) { "Mangler endringsType i aktivitet-melding" }.name,
                fraDato = DateUtils.dateToTimestamp(fraDato),
                tilDato = DateUtils.dateToTimestamp(tilDato),
                endretDato = DateUtils.dateToTimestamp(endretDato),
                tiltakskode = tiltakskode,
                lagtInnAv = requireNotNull(lagtInnAv) { "Mangler lagtInnAv i aktivitet-melding" }.name,
                avtalt = avtalt,
                version = requireNotNull(version) { "Mangler version i aktivitet-melding" },
                historisk = historisk,
                cvKanDelesStatus = stillingFraNavData?.cvKanDelesStatus?.name,
                svarfristStillingFraNav = stillingFraNavData?.svarfrist?.toLocalDate()?.let(DateUtils::toTimestamp),
            ),
            metadata = metadata,
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class StillingFraNavPortefoljeData(
        @param:JsonProperty("cvKanDelesStatus")
        val cvKanDelesStatus: CvKanDelesStatus? = null,
        @param:JsonProperty("svarfrist")
        val svarfrist: java.sql.Date? = null,
    )

    enum class AktivitetStatus {
        PLANLAGT,
        GJENNOMFORES,
        FULLFORT,
        BRUKER_ER_INTERESSERT,
        AVBRUTT,
    }

    enum class AktivitetTypeDTO {
        EGEN,
        STILLING,
        SOKEAVTALE,
        IJOBB,
        BEHANDLING,
        MOTE,
        SAMTALEREFERAT,
        STILLING_FRA_NAV,
        TILTAK,
    }

    enum class EndringsType {
        OPPRETTET,
        FLYTTET,
        REDIGERT,
        HISTORISK,
    }

    enum class Innsender {
        BRUKER,
        NAV,
        ARBEIDSGIVER,
        TILTAKSARRANGOER,
        ARENAIDENT,
        SYSTEM,
    }

    enum class CvKanDelesStatus {
        JA,
        NEI,
        IKKE_SVART,
    }
}

data class KafkaAktivitetMeldingEntity(
    val value: KafkaAktivitetMeldingValue,
    val metadata: KafkaMeldingMetadata,
)

data class KafkaAktivitetMeldingValue(
    val aktivitetId: String,
    val aktorId: String,
    val aktivitetType: String,
    val aktivitetStatus: String,
    val endringsType: String,
    val fraDato: Timestamp?,
    val tilDato: Timestamp?,
    val endretDato: Timestamp?,
    val tiltakskode: String?,
    val lagtInnAv: String,
    val avtalt: Boolean,
    val version: Long,
    val historisk: Boolean,
    val cvKanDelesStatus: String?,
    val svarfristStillingFraNav: Timestamp?,
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
                "Kafka-record mangler key for aktivitetId=${value.aktivitetId}"
            },
        ),
    )