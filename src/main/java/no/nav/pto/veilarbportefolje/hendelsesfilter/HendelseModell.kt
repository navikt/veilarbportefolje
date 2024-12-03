import no.nav.common.types.identer.NorskIdent
import java.net.URL
import java.time.ZonedDateTime
import java.util.*

enum class Kategori {
    UTGATT_VARSEL
}

enum class Operasjon {
    START,
    STOPP,
    OPPDATER
}

data class Hendelse(
    val id: UUID,
    val personIdent: NorskIdent,
    val avsender: String,
    val kategori: Kategori,
    val hendelse: HendelseInnhold
) {
    data class HendelseInnhold(
        val beskrivelse: String,
        val dato: ZonedDateTime,
        val lenke: URL,
        val detaljer: String?
    )
}
