package no.nav.pto.veilarbportefolje.postgres

import no.nav.pto.veilarbportefolje.aap.AapRepository
import no.nav.pto.veilarbportefolje.aap.domene.AapVedtakEntity
import no.nav.pto.veilarbportefolje.dagpenger.DagpengerRepository
import no.nav.pto.veilarbportefolje.dagpenger.domene.DagpengerVedtakEntity
import no.nav.pto.veilarbportefolje.domene.opensearchmodell.DagpengerForOpensearch
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.FnrIdent
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class PostgresYtelserMapper(
    private val aapRepository: AapRepository,
    private val dagpengerRepository: DagpengerRepository,
    private val pdlIdentRepository: PdlIdentRepository
) {

    fun flettInnYtelser(brukere: List<PortefoljebrukerOpensearchModell>) {
        val fnrIdenterPerBruker = hentFnrIdenterPerBruker(brukere)
        flettInnAapKelvin(brukere, fnrIdenterPerBruker)
        flettInnDagpenger(brukere, fnrIdenterPerBruker)
    }

    private fun flettInnAapKelvin(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<String, List<FnrIdent>>
    ) {
        val aapVedtakForAlleIdenterPerBruker =
            aapRepository.hentAapKelvinVedtak(alleIdenter(fnrIdenterPerBruker))

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val aktueltVedtak = velgVedtak("AAP", bruker.fnr, identer, aapVedtakForAlleIdenterPerBruker, AapVedtakEntity::radSistEndret)
            val vedtakErGjeldende = aktueltVedtak != null && aktueltVedtak.nyestePeriodeTom.isAfter(LocalDate.now().minusDays(1))

            bruker.aap_kelvin = vedtakErGjeldende
            bruker.aap_kelvin_tom_vedtaksdato = if (vedtakErGjeldende) aktueltVedtak.nyestePeriodeTom else null
            bruker.aap_kelvin_rettighetstype = if (vedtakErGjeldende) aktueltVedtak.rettighetstype else null
            bruker.aap_kelvin_maksdato = if (vedtakErGjeldende) aktueltVedtak.maksdato else null
        }
    }

    private fun flettInnDagpenger(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<String, List<FnrIdent>>
    ) {
        val dagpengerVedtakForAlleIdenterPerBruker =
            dagpengerRepository.hentDagpengerVedtak(alleIdenter(fnrIdenterPerBruker))

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val aktueltVedtak = velgVedtak("dagpenger", bruker.fnr, identer, dagpengerVedtakForAlleIdenterPerBruker, DagpengerVedtakEntity::radSistEndret)
            val vedtakErGjeldende = aktueltVedtak != null &&
                    (aktueltVedtak.nyestePeriodeTom == null || aktueltVedtak.nyestePeriodeTom.isAfter(LocalDate.now().minusDays(1)))

            bruker.dagpenger = DagpengerForOpensearch(
                harDagpenger = vedtakErGjeldende,
                rettighetstype = if (vedtakErGjeldende) aktueltVedtak.rettighetstype else null,
                datoStans = if (vedtakErGjeldende) aktueltVedtak.nyestePeriodeTom else null,
                antallResterendeDager = if (vedtakErGjeldende) aktueltVedtak.antallResterendeDager else null
            )
        }
    }

    private fun hentFnrIdenterPerBruker(brukere: List<PortefoljebrukerOpensearchModell>): Map<String, List<FnrIdent>> {
        val fnrs = brukere.mapNotNull { it.fnr }
        val fnrIdenterPerBruker = pdlIdentRepository.hentFnrIdenterForBrukere(fnrs)
        fnrs.filterNot { fnrIdenterPerBruker.containsKey(it) }.forEach { fnr ->
            secureLog.error("Fant ingen folkeregisteridenter i bruker_identer for fnr: $fnr")
        }
        return fnrIdenterPerBruker
    }

    private fun alleIdenter(fnrIdenterPerBruker: Map<String, List<FnrIdent>>): List<String> =
        fnrIdenterPerBruker.values.flatten().map { it.fnr }.distinct()

    // Hvis det finnes flere rader så skal aktiv ident prioriteres. Deretter velges vedtak med siste "rad sist endret" tidspunkt (vil nok aldri inntreffe).
    private fun <T> velgVedtak(
        ytelse: String,
        fnr: String?,
        identer: List<FnrIdent>,
        vedtak: Map<String, T>,
        radSistEndret: (T) -> LocalDateTime
    ): T? {
        val identerMedVedtak = identer.filter { vedtak.containsKey(it.fnr) }
        if (identerMedVedtak.size > 1) {
            secureLog.warn("Fant $ytelse på flere fnr for samme person. Oppslag: $fnr, identer med $ytelse: ${identerMedVedtak.map { it.fnr }}")
        }
        val valgtIdent = identerMedVedtak.firstOrNull { !it.historisk }
            ?: identerMedVedtak.maxByOrNull { radSistEndret(vedtak.getValue(it.fnr)) }
        return valgtIdent?.let { vedtak[it.fnr] }
    }
}
