package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

import no.nav.pto.veilarbportefolje.domene.filtervalg.DinSituasjonSvar

fun mapTilUtdanning(nus: String?): Utdanning {
    return when (nus) {
        "0" -> Utdanning.INGEN_UTDANNING
        "2" -> Utdanning.GRUNNSKOLE
        "3" -> Utdanning.VIDEREGAENDE_GRUNNUTDANNING
        "4" -> Utdanning.VIDEREGAENDE_FAGBREV_SVENNEBREV
        "6" -> Utdanning.HOYERE_UTDANNING_1_TIL_4
        "7" -> Utdanning.HOYERE_UTDANNING_5_ELLER_MER
        else -> Utdanning.INGEN_SVAR
    }
}


fun mapJobbsituasjonTilSituasjonFraGammelRegistrering(jobbsituasjon: String?): List<String> {
    if(JobbSituasjonBeskrivelse.HAR_BLITT_SAGT_OPP.name == jobbsituasjon) {
        return listOf(JobbSituasjonBeskrivelse.HAR_BLITT_SAGT_OPP.name, DinSituasjonSvar.MISTET_JOBBEN.name, DinSituasjonSvar.OPPSIGELSE.name, )
    }
    if(JobbSituasjonBeskrivelse.ER_PERMITTERT.name == jobbsituasjon) {
        return listOf(JobbSituasjonBeskrivelse.ER_PERMITTERT.name, DinSituasjonSvar.ENDRET_PERMITTERINGSPROSENT.name, DinSituasjonSvar.TILBAKE_TIL_JOBB.name )
    }
    if(JobbSituasjonBeskrivelse.HAR_SAGT_OPP.name == jobbsituasjon) {
        return listOf(JobbSituasjonBeskrivelse.HAR_SAGT_OPP.name, DinSituasjonSvar.SAGT_OPP.name)
    }
    if(JobbSituasjonBeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR.name == jobbsituasjon) {
        return listOf(JobbSituasjonBeskrivelse.IKKE_VAERT_I_JOBB_SISTE_2_AAR.name, DinSituasjonSvar.JOBB_OVER_2_AAR.name)
    }
    if(JobbSituasjonBeskrivelse.ANNET.name == jobbsituasjon) {
        return listOf(JobbSituasjonBeskrivelse.ANNET.name, DinSituasjonSvar.VIL_FORTSETTE_I_JOBB.name)
    }
    return if (jobbsituasjon != null) listOf(jobbsituasjon) else emptyList()

}