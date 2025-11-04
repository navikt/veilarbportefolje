package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat

data class Etiketter(
    var erDoed: Boolean = false,
    val erSykmeldtMedArbeidsgiver: Boolean = false,
    val trengerOppfolgingsvedtak: Boolean = false,
    val nyForVeileder: Boolean = false,
    val nyForEnhet: Boolean = false,
    val harBehovForArbeidsevneVurdering: Boolean = false,
    val harSikkerhetstiltak: Boolean = false,
    var diskresjonskodeFortrolig: String? = null,
    val profileringResultat: Profileringsresultat? = null,
)
