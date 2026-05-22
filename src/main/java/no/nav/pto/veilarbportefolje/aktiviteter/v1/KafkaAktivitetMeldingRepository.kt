package no.nav.pto.veilarbportefolje.aktiviteter.v1

import no.nav.pto.veilarbportefolje.aktiviteter.domene.AktivitetIkkeAktivStatuser
import org.springframework.stereotype.Component
import kotlin.enums.EnumEntries

@Component
class KafkaAktivitetMeldingRepository {
    fun hentTiltaksaktiviteter(
        personidenter: List<String>,
        avtalt: Boolean,
        aktivitetStatusFilter: List<EnumEntries<AktivitetIkkeAktivStatuser>>
    ): Map<String, List<String>> {
        TODO("Not yet implemented")
    }
}
