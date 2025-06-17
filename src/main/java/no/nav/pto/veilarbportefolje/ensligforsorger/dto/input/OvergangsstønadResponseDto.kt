package no.nav.pto.veilarbportefolje.ensligforsorger.dto.input;

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class OvergangsstønadBarn @JsonCreator constructor(
        @JsonProperty("personIdent")
        val personIdent: String,
        @JsonProperty("fødselTermindato")
        val fødselTermindato: LocalDate
)

data class OvergangsstønadPeriode @JsonCreator constructor(
        @JsonProperty("stønadFraOgMed")
        val stønadFraOgMed: LocalDate,
        @JsonProperty("stønadTilOgMed")
        val stønadTilOgMed: LocalDate,
        @JsonProperty("aktivitet")
        val aktivitet: Aktivitetstype,
        @JsonProperty("periodeType")
        val periodeType: Periodetype,
        @JsonProperty("barn")
        val barn: List<OvergangsstønadBarn>,
        @JsonProperty("behandlingId")
        val behandlingId: Long,
        @JsonProperty("harAktivitetsplikt")
        val harAktivitetsplikt: Boolean
)

data class OvergangsstønadData @JsonCreator constructor(
        @JsonProperty("personIdent")
        val personIdent: List<String>,
        @JsonProperty("perioder")
        val perioder: List<OvergangsstønadPeriode>
)


data class OvergangsstønadResponseDto @JsonCreator constructor(
        @JsonProperty("data")
        val data: OvergangsstønadData,
        @JsonProperty("status")
        val status: String,
        @JsonProperty("melding")
        val melding: String,
        @JsonProperty("frontendFeilmelding")
        val frontendFeilmelding: String?,
        @JsonProperty("stacktrace")
        val stacktrace: String?,
        @JsonProperty("callId")
        val callId: String?
)
