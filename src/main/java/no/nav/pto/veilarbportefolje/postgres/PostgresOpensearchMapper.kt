package no.nav.pto.veilarbportefolje.postgres

import no.nav.pto.veilarbportefolje.aktiviteter.domene.AktivitetIkkeAktivStatuser
import no.nav.pto.veilarbportefolje.aktiviteter.v1.KafkaAktivitetMeldingRepository
import no.nav.pto.veilarbportefolje.opensearch.domene.AktivitetData
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import org.springframework.stereotype.Component

@Component
class PostgresOpensearchMapperV2(
    val kafkaAktivitetMeldingRepository: KafkaAktivitetMeldingRepository
) {
    fun flettInnAktivitetData(brukerOpensearchModellList: List<PortefoljebrukerOpensearchModell>): List<PortefoljebrukerOpensearchModell> {
        val aktorIder = brukerOpensearchModellList.mapNotNull { it.aktoer_id }
        val tiltak = kafkaAktivitetMeldingRepository.hentTiltaksaktiviteter(
            personidenter = aktorIder,
            avtalt = true,
            aktivitetStatusFilter = listOf(
                AktivitetIkkeAktivStatuser.entries
            )
        )

        return brukerOpensearchModellList.map {
            it.copy(
                aktivitetData = AktivitetData(
                    tiltak.keys.toSet()
                )
            )
        }
    }
}