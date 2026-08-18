package no.nav.pto.veilarbportefolje.hendelsesfilter

import no.nav.common.types.identer.NorskIdent
import java.net.URL
import java.time.ZonedDateTime
import java.util.*

data class HendelseRecordValue(
    val personID: NorskIdent,
    val avsender: String,
    val kategori: Kategori,
    val operasjon: Operasjon,
    val hendelse: HendelseInnhold
) {
    data class HendelseInnhold(
        // Det er produsent som må bestemme kobling mellom beskrivelse og beskrivelseEnum.
        // Førstnenvte er tekst som vises i frontend, og enum er for lettere sortering og filtrering i backend.
        val beskrivelse: String,
        val beskrivelseEnum: String?,
        val dato: ZonedDateTime,
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
        val beskrivelseEnum: String?,
        val dato: ZonedDateTime,
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
        hendelse = Hendelse.HendelseInnhold(
            beskrivelse = hendelseRecordValue.hendelse.beskrivelse,
            beskrivelseEnum = hendelseRecordValue.hendelse.beskrivelseEnum,
            dato = hendelseRecordValue.hendelse.dato,
            lenke = hendelseRecordValue.hendelse.lenke,
            detaljer = hendelseRecordValue.hendelse.detaljer,
        )
    )
}
