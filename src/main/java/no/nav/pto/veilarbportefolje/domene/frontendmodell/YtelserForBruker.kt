package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.domene.YtelseMapping
import java.time.LocalDate
import java.time.LocalDateTime


data class YtelserForBruker(
    val innsatsgruppe: String? = null, // aap arena, sjekker på gruppe BATT
    val ytelse: YtelseMapping? = null,
    val utlopsdato: LocalDateTime? = null,
    val dagputlopUke: Int? = null,
    val permutlopUke: Int? = null,
    val aapmaxtidUke: Int? = null,
    val aapUnntakUkerIgjen: Int? = null,
    val aapordinerutlopsdato: LocalDate? = null,
    val aapKelvin: AapKelvinForBruker? = null,
    val tiltakspenger: TiltakspengerForBruker? = null,
    val ensligeForsorgereOvergangsstonad: EnsligeForsorgereOvergangsstonadFrontend? = null,
)
