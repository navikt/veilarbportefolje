package no.nav.pto.veilarbportefolje.domene.frontendmodell

import java.time.LocalDate

data class GeografiskBostedForBruker(
    var bostedKommune: String?,
    val bostedKommuneUkjentEllerUtland: String,
    var bostedBydel: String?,
    var bostedSistOppdatert: LocalDate?
)
