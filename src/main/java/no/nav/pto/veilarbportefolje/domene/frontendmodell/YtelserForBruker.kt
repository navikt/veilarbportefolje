package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.domene.YtelseMapping
import java.time.LocalDate
import java.time.LocalDateTime


data class YtelserForBruker(
    val ytelserArena : YtelserArena,
    val aap: AapKelvinForBruker?,
    val tiltakspenger: TiltakspengerForBruker?,
    val ensligeForsorgereOvergangsstonad: EnsligeForsorgereOvergangsstonadFrontend?,
)


data class YtelserArena(
    val innsatsgruppe: String?, // aap arena, sjekker på gruppe BATT
    val ytelse: YtelseMapping?,
    val utlopsdato: LocalDateTime?,
    val dagputlopUke: Int?,
    val permutlopUke: Int?,
    val aapmaxtidUke: Int?,
    val aapUnntakUkerIgjen: Int?,
    val aapordinerutlopsdato: LocalDate?,
)
