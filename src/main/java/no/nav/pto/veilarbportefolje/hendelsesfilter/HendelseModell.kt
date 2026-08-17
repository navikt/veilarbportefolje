package no.nav.pto.veilarbportefolje.hendelsesfilter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.common.types.identer.NorskIdent
import java.net.URL
import java.time.ZonedDateTime
import java.util.*

/* Kafka-spesifikke typer */
/*
* Her må vi bruke en kombinasjon av @JsonCreator og @JsonProperty
* siden vi bruker Kotlin data classes og ikke kontrollerer deserialiseringen som skjer ved konsumering
* av Kafka-records. I skrivende stund er det kafka-modulen fra common-java-modules
* som styrer dette, hvor også FasterXML Jackson er det underliggende biblioteket som brukes.
* Vi kunne i stor grad ha unngått bruken av disse annotasjonene dersom vi hadde mulighet til å
* registrere jackson-module-kotlin i object-mapperen som brukes i common-java-modules.
* */
data class HendelseRecordValue @JsonCreator constructor(
    @JsonProperty("personID")
    val personID: NorskIdent,
    @JsonProperty("avsender")
    val avsender: String,
    @JsonProperty("kategori")
    val kategori: Kategori,
    @JsonProperty("operasjon")
    val operasjon: Operasjon,
    @JsonProperty(value = "hendelse")
    val hendelse: HendelseInnhold
) {
    data class HendelseInnhold @JsonCreator constructor(
        // Det er produsent som må bestemme kobling mellom beskrivelse og beskrivelseEnum.
        // Førstnenvte er tekst som vises i frontend, og enum er for lettere sortering og filtrering i backend.
        @JsonProperty("beskrivelse")
        val beskrivelse: String,
        @JsonProperty("beskrivelseEnum")
        val beskrivelseEnum: String?,
        @JsonProperty("dato")
        val dato: ZonedDateTime,
        @JsonProperty("datoFrist")
        val datoFrist: ZonedDateTime?,
        @JsonProperty("lenke")
        val lenke: URL,
        @JsonProperty("detaljer")
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

data class Hendelse @JsonCreator constructor(
    @JsonProperty("id")
    val id: UUID,
    @JsonProperty("personIdent")
    val personIdent: NorskIdent,
    @JsonProperty("avsender")
    val avsender: String,
    @JsonProperty("kategori")
    val kategori: Kategori,
    @JsonProperty("hendelse")
    val hendelse: HendelseInnhold
) {
    data class HendelseInnhold @JsonCreator constructor(
        @JsonProperty("beskrivelse")
        val beskrivelse: String,
        @JsonProperty("beskrivelseEnum")
        val beskrivelseEnum: String?,
        @JsonProperty("dato")
        val dato: ZonedDateTime,
        @JsonProperty("datoFrist")
        val datoFrist: ZonedDateTime?,
        @JsonProperty("lenke")
        val lenke: URL,
        @JsonProperty("detaljer")
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
            datoFrist = hendelseRecordValue.hendelse.datoFrist,
            lenke = hendelseRecordValue.hendelse.lenke,
            detaljer = hendelseRecordValue.hendelse.detaljer,
        )
    )
}
