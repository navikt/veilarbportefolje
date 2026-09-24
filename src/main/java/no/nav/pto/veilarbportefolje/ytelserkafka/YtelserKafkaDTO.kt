package no.nav.pto.veilarbportefolje.ytelserkafka


data class YtelserKafkaDTO(
    val personId: String,
    val meldingstype: YTELSE_MELDINGSTYPE,
    val ytelsestype: YTELSE_TYPE,
    val kildesystem: YTELSE_KILDESYSTEM
)

enum class YTELSE_MELDINGSTYPE {
    OPPRETT,
    OPPDATER,
    SLETT
}

enum class YTELSE_KILDESYSTEM {
    KELVIN,
    TPSAK,
    DPSAK,
    XXX
}

enum class YTELSE_TYPE {
    AAP,
    TILTAKSPENGER,
    DAGPENGER,
    UFORETRYGD
}
