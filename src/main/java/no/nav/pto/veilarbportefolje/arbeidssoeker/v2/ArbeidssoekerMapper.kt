package no.nav.pto.veilarbportefolje.arbeidssoeker.v2

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