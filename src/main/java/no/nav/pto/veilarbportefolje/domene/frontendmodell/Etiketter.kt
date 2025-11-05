package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat

data class Etiketter(
    val erDoed: Boolean,
    val erSykmeldtMedArbeidsgiver: Boolean,
    val trengerOppfolgingsvedtak: Boolean,
    val nyForVeileder: Boolean,
    val nyForEnhet: Boolean,
    val harBehovForArbeidsevneVurdering: Boolean,
    val harSikkerhetstiltak: Boolean,
    var diskresjonskodeFortrolig: String? = null,
    val profileringResultat: Profileringsresultat? = null,
)
