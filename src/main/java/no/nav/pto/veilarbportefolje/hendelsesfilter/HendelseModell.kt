package no.nav.pto.veilarbportefolje.hendelsesfilter

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.common.types.identer.EksternBrukerId
import no.nav.common.types.identer.Fnr
import no.nav.common.types.identer.NorskIdent
import java.time.LocalDateTime
import java.util.*

/* Kafka-spesifikke typer */
data class HendelseRecordValue(
    val personID: String,
    val avsender: String,
    val kategori: Kategori,
    val operasjon: Operasjon,
    @JsonProperty(value = "hendelse")
    val hendelseInnhold: HendelseInnhold
)

data class HendelseInnhold(
    val navn: String,
    val dato: LocalDateTime,
    val lenke: String,
    val detaljer: String
)

enum class Kategori {
    UTGATT_VARSEL
}

enum class Operasjon {
    START,
    STOPP,
    ENDRING
}

/* Interne typer */
data class Hendelse(
    val id: UUID,
    val personIdent: NorskIdent,
    val avsender: String,
    val kategori: Kategori,
    val operasjon: Operasjon,
    val hendelseInnhold: HendelseInnhold
)

fun toHendelse(hendelseRecordValue: HendelseRecordValue, hendelseKey: String): Hendelse {
    return Hendelse(
        id = UUID.fromString(hendelseKey),
        personIdent = NorskIdent.of(hendelseRecordValue.personID),
        avsender = hendelseRecordValue.avsender,
        kategori = hendelseRecordValue.kategori,
        operasjon = hendelseRecordValue.operasjon,
        hendelseInnhold = hendelseRecordValue.hendelseInnhold
    )
}