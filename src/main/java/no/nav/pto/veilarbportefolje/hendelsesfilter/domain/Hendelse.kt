package no.nav.pto.veilarbportefolje.hendelsesfilter.domain

import com.fasterxml.jackson.annotation.JsonProperty

data class Hendelse(
    val personID: String,
    val avsender: String,
    val kategori: Kategori,
    val operasjon: Operasjon,
    @JsonProperty(value = "hendelse")
    val hendelseInnhold: HendelseInnhold
)