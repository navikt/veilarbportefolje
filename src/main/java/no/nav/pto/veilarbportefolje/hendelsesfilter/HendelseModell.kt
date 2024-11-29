package no.nav.pto.veilarbportefolje.hendelsesfilter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.common.types.identer.NorskIdent
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.*

/* Kafka-spesifikke typer */
/*
* Vi må her bruke en kombinasjon av @JsonCreator og @JsonProperty
* siden vi ikke kontrollerer deserialiseringen som skjer ved konsumering
* av Kafka-records. I skrivende stund er det kafka-modulen fra common-java-modules
* som styrer dette, hvor også FasterXML Jackson er det underliggende biblioteket som brukes.
* Vi kunne i stor grad ha unngått bruken av disse annotasjonene dersom vi hadde mulighet til å
* registrere jackson-module-kotlin i object-mapperen som brukes i common-java-modules.
* */
data class HendelseRecordValue @JsonCreator constructor(
    @JsonProperty("personID")
    val personID: String,
    @JsonProperty("avsender")
    val avsender: String,
    @JsonProperty("kategori")
    val kategori: Kategori,
    @JsonProperty("operasjon")
    val operasjon: Operasjon,
    @JsonProperty(value = "hendelse")
    val hendelseInnhold: HendelseInnhold
)

data class HendelseInnhold @JsonCreator constructor(
    @JsonProperty("navn")
    val navn: String,
    @JsonProperty("dato")
    val dato: ZonedDateTime,
    @JsonProperty("lenke")
    val lenke: String,
    @JsonProperty("detaljer")
    val detaljer: String?
)

enum class Kategori {
    UTGATT_VARSEL
}

enum class Operasjon {
    START,
    STOPP,
    OPPDATER
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