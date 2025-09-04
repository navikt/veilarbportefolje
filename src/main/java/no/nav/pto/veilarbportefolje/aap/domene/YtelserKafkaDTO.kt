package no.nav.pto.veilarbportefolje.aap.domene


data class YtelserKafkaDTO(
    val personId: String,
    val meldingstype: YTELSE_MELDINGSTYPE,
    val ytelsestype: YTELSE_TYPE,
    val kildesystem: YTELSE_KILDESYSTEM
)


enum class YTELSE_MELDINGSTYPE {
    OPPRETT,
    OPPDATER
}

enum class YTELSE_KILDESYSTEM {
    KELVIN
}

enum class YTELSE_TYPE {
    AAP
}
