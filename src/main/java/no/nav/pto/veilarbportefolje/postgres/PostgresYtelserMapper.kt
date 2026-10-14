package no.nav.pto.veilarbportefolje.postgres

import no.nav.pto.veilarbportefolje.aap.AapRepository
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PostgresYtelserMapper(private val aapRepository: AapRepository) {

    fun flettInnAapKelvin(brukere: List<PortefoljebrukerOpensearchModell>) {
        val aapVedtak = aapRepository.hentAapKelvinVedtak(brukere.mapNotNull { it.fnr })

        brukere.forEach { bruker ->
            val gjeldendeVedtak = bruker.fnr
                ?.let { aapVedtak[it] }
                ?.takeIf { it.nyestePeriodeTom.isAfter(LocalDate.now().minusDays(1)) }

            bruker.aap_kelvin = gjeldendeVedtak != null
            bruker.aap_kelvin_tom_vedtaksdato = gjeldendeVedtak?.nyestePeriodeTom
            bruker.aap_kelvin_rettighetstype = gjeldendeVedtak?.rettighetstype
            bruker.aap_kelvin_maksdato = gjeldendeVedtak?.maksdato
        }
    }
}
