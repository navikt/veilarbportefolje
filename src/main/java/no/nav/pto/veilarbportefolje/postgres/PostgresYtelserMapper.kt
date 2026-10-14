package no.nav.pto.veilarbportefolje.postgres

import no.nav.pto.veilarbportefolje.aap.AapRepository
import no.nav.pto.veilarbportefolje.aap.domene.AapKelvinVedtak
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.FnrIdent
import no.nav.pto.veilarbportefolje.util.SecureLog.secureLog
import org.springframework.stereotype.Service

@Service
class PostgresYtelserMapper(
    private val aapRepository: AapRepository,
    private val pdlIdentRepository: PdlIdentRepository
) {

    fun flettInnAapKelvin(brukere: List<PortefoljebrukerOpensearchModell>) {
        val fnrIdenterPerBruker = hentFnrIdenterPerBruker(brukere)
        val aapVedtakForAlleIdenterPerBruker =
            aapRepository.hentAapKelvinVedtak(fnrIdenterPerBruker.values.flatten().map { it.fnr }.distinct())

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val gjeldendeVedtak = velgAapVedtak(bruker.fnr, identer, aapVedtakForAlleIdenterPerBruker)

            bruker.aap_kelvin = gjeldendeVedtak != null
            bruker.aap_kelvin_tom_vedtaksdato = gjeldendeVedtak?.nyestePeriodeTom
            bruker.aap_kelvin_rettighetstype = gjeldendeVedtak?.rettighetstype
            bruker.aap_kelvin_maksdato = gjeldendeVedtak?.maksdato
        }
    }

    private fun hentFnrIdenterPerBruker(brukere: List<PortefoljebrukerOpensearchModell>): Map<String, List<FnrIdent>> {
        val fnrs = brukere.mapNotNull { it.fnr }
        val fnrIdenterPerBruker = pdlIdentRepository.hentFnrIdenterForBrukere(fnrs)
        return fnrs.associateWith { fnr ->
            fnrIdenterPerBruker[fnr] ?: listOf(FnrIdent(fnr, historisk = false))
        }
    }

    // Hvis det finnes flere rader så skal aktiv ident prioriteres. Deretter velges vedtak med siste "rad sist endret" tidspunkt (vil nok aldri inntreffe).
    private fun velgAapVedtak(
        fnr: String?,
        identer: List<FnrIdent>,
        aapVedtak: Map<String, AapKelvinVedtak>
    ): AapKelvinVedtak? {
        val identerMedAap = identer.filter { aapVedtak.containsKey(it.fnr) }
        if (identerMedAap.size > 1) {
            secureLog.warn("Fant AAP på flere fnr for samme person. Oppslag: $fnr, identer med AAP: ${identerMedAap.map { it.fnr }}")
        }
        val valgtIdent = identerMedAap.firstOrNull { !it.historisk }
            ?: identerMedAap.maxByOrNull { aapVedtak.getValue(it.fnr).radSistEndret }
        return valgtIdent?.let { aapVedtak[it.fnr] }
    }
}
