package no.nav.pto.veilarbportefolje.domene.opensearchmodell

import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene.Profileringsresultat
import no.nav.pto.veilarbportefolje.arbeidssoeker.v3.domene.Utdanning
import java.time.LocalDate

data class ArbeidssoekerregisteretForOpensearch(
    val periodeStartDato: LocalDate,
    val periodeAvsluttetDato: LocalDate? = null,
    val opplysninger: OpplysningerOmArbeidssoekerForOpensearch? = null,
    val profilering: ProfileringForOpensearch? = null,
)

data class OpplysningerOmArbeidssoekerForOpensearch(
    val brukersSituasjoner: List<String>? = null,
    val utdanning: Utdanning? = null,
    val utdanningBestatt: String? = null,
    val utdanningGodkjent: String? = null,
    val opplysningerSistSendtInnDato: LocalDate,
)

data class ProfileringForOpensearch(
    val profileringsResultat: Profileringsresultat,
)
