package no.nav.pto.veilarbportefolje.hendelsesfilter

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.common.types.identer.NorskIdent
import java.net.URL
import java.time.ZonedDateTime
import java.util.*

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "schemaVersjon",
    defaultImpl = HendelseRecordValueV1::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = HendelseRecordValueV1::class, name = "V1"),
    JsonSubTypes.Type(value = HendelseRecordValueV2::class, name = "V2"),
)
sealed interface HendelseRecordValue {
    val personID: NorskIdent
    val avsender: String
    val kategori: Kategori
    val operasjon: Operasjon
}

data class HendelseRecordValueV1(
    override val personID: NorskIdent,
    override val avsender: String,
    override val kategori: Kategori,
    override val operasjon: Operasjon,
    val hendelse: HendelseInnhold,
) : HendelseRecordValue {
    data class HendelseInnhold(
        // Det er produsent som må bestemme kobling mellom beskrivelse og beskrivelseEnum.
        // Førstnenvte er tekst som vises i frontend, og enum er for lettere sortering og filtrering i backend.
        val beskrivelse: String,
        val beskrivelseEnum: String? = null,
        val dato: ZonedDateTime,
        val datoFrist: ZonedDateTime? = null,
        val lenke: URL,
        val detaljer: String?
    )
}

data class HendelseRecordValueV2(
    override val personID: NorskIdent,
    override val avsender: String,
    override val kategori: Kategori,
    override val operasjon: Operasjon,
    val hendelse: HendelseInnhold?,
) : HendelseRecordValue {
    init {
        if (operasjon != Operasjon.STOPP) {
            require(hendelse != null) {
                "HendelseRecordValueV2.hendelse må være satt når operasjon er $operasjon."
            }
        }

        if (operasjon == Operasjon.STOPP) {
            require(hendelse == null) {
                "HendelseRecordValueV2.hendelse må være null når operasjon er $operasjon."
            }
        }
    }

    data class HendelseInnhold(
        // Det er produsent som må bestemme kobling mellom beskrivelse og beskrivelseEnum.
        // Førstnenvte er tekst som vises i frontend, og enum er for lettere sortering og filtrering i backend.
        val beskrivelse: String,
        val beskrivelseEnum: String? = null,
        val tidspunkt: ZonedDateTime,
        val tidspunktFrist: ZonedDateTime? = null,
        val lenke: URL,
        val detaljer: String?
    )
}

enum class Kategori {
    UTGATT_VARSEL,
    UDELT_SAMTALEREFERAT,
    KANDIDAT_FOR_UTMELDING
}

enum class Operasjon {
    START,
    STOPP,
    OPPDATER
}

data class Hendelse(
    val id: UUID,
    val personIdent: NorskIdent,
    val avsender: String,
    val kategori: Kategori,
    val hendelse: HendelseInnhold
) {
    data class HendelseInnhold(
        val beskrivelse: String,
        val beskrivelseEnum: String? = null,
        val dato: ZonedDateTime,
        val datoFrist: ZonedDateTime? = null,
        val lenke: URL,
        val detaljer: String?
    )
}

fun toHendelse(hendelseRecordValue: HendelseRecordValue, hendelseKey: String): Hendelse {
    return Hendelse(
        id = UUID.fromString(hendelseKey),
        personIdent = hendelseRecordValue.personID,
        avsender = hendelseRecordValue.avsender,
        kategori = hendelseRecordValue.kategori,
        hendelse = toHendelseInnhold(hendelseRecordValue)
    )
}

private fun toHendelseInnhold(hendelseRecordValue: HendelseRecordValue): Hendelse.HendelseInnhold {
    return when (hendelseRecordValue) {
        is HendelseRecordValueV1 -> {
            Hendelse.HendelseInnhold(
                beskrivelse = hendelseRecordValue.hendelse.beskrivelse,
                beskrivelseEnum = hendelseRecordValue.hendelse.beskrivelseEnum,
                dato = hendelseRecordValue.hendelse.dato,
                datoFrist = hendelseRecordValue.hendelse.datoFrist,
                lenke = hendelseRecordValue.hendelse.lenke,
                detaljer = hendelseRecordValue.hendelse.detaljer,
            )
        }

        is HendelseRecordValueV2 -> {
            val hendelseInnhold = requireNotNull(hendelseRecordValue.hendelse) {
                "HendelseInnhold må være satt for HendelseRecordValueV2 ved operasjon ${hendelseRecordValue.operasjon}."
            }

            Hendelse.HendelseInnhold(
                beskrivelse = hendelseInnhold.beskrivelse,
                beskrivelseEnum = hendelseInnhold.beskrivelseEnum,
                dato = hendelseInnhold.tidspunkt,
                datoFrist = hendelseInnhold.tidspunktFrist,
                lenke = hendelseInnhold.lenke,
                detaljer = hendelseInnhold.detaljer,
            )
        }
    }
}
