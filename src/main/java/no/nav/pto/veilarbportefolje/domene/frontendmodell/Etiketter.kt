package no.nav.pto.veilarbportefolje.domene.frontendmodell

import no.nav.pto.veilarbportefolje.arbeidssoeker.v2.Profileringsresultat
import java.time.LocalDate

data class Etiketter(
    val erDoed: Boolean,
    val trengerOppfolgingsvedtak: Boolean,
    val nyForVeileder: Boolean,
    val nyForEnhet: Boolean,
    val harSikkerhetstiltak: Boolean,
    var diskresjonskodeFortrolig: String?, // fjernes etter vi bytter til adressebeskyttelse
    val adressebeskyttelse: String?,
    val egenAnsatt: Boolean,
    val skjermetTil: LocalDate?,
    val profileringResultat: Profileringsresultat?,
    val kandidatForUtmelding: Boolean,
    val sykmeldtMedArbeidsgiver: Boolean,
    val sykmeldtUtenArbeidsgiver: Boolean,
)
