package no.nav.pto.veilarbportefolje.postgres

import no.nav.poao_tilgang.client.NorskIdent
import no.nav.pto.veilarbportefolje.domene.HuskelappForBruker
import no.nav.pto.veilarbportefolje.fargekategori.FargekategoriRepository
import no.nav.pto.veilarbportefolje.huskelapp.HuskelappRepository
import no.nav.pto.veilarbportefolje.opensearch.domene.PortefoljebrukerOpensearchModell
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository
import no.nav.pto.veilarbportefolje.persononinfo.domene.NorskIdentMedHistorikk
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class PostgresHuskelappOgFargekategoriMapper(
    private val huskelappRepository: HuskelappRepository,
    private val fargekategoriRepository: FargekategoriRepository,
    private val pdlIdentRepository: PdlIdentRepository
) {

    fun flettInnHuskelappOgFargekategori(brukere: List<PortefoljebrukerOpensearchModell>) {
        val fnrIdenterPerBruker = hentFnrIdenterPerBruker(pdlIdentRepository, brukere)
        flettInnHuskelapp(brukere, fnrIdenterPerBruker)
        flettInnFargekategori(brukere, fnrIdenterPerBruker)
    }

    private fun flettInnHuskelapp(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>
    ) {
        val aktivHuskelappPerIdent = huskelappRepository.hentAktivHuskelappPerIdent(alleIdenter(fnrIdenterPerBruker))

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val huskelapp = velgDataForPrioritertIdent(
                "huskelapp",
                bruker.fnr,
                identer,
                aktivHuskelappPerIdent,
                { it.endretDato()?.atStartOfDay() ?: LocalDateTime.MIN }
            )

            bruker.huskelapp = huskelapp
                ?.takeIf { it.frist() != null || it.kommentar() != null }
                ?.let {
                    HuskelappForBruker(
                        it.frist(),
                        it.kommentar(),
                        it.endretDato(),
                        it.endretAv().value,
                        it.huskelappId()?.toString(),
                        it.enhetId().get()
                    )
                }
        }
    }

    private fun flettInnFargekategori(
        brukere: List<PortefoljebrukerOpensearchModell>,
        fnrIdenterPerBruker: Map<NorskIdent, List<NorskIdentMedHistorikk>>
    ) {
        val fargekategoriPerIdent = fargekategoriRepository.hentFargekategoriPerIdent(alleIdenter(fnrIdenterPerBruker))

        brukere.forEach { bruker ->
            val identer = bruker.fnr?.let { fnrIdenterPerBruker[it] } ?: emptyList()
            val fargekategori = velgDataForPrioritertIdent(
                "fargekategori",
                bruker.fnr,
                identer,
                fargekategoriPerIdent,
                { it.sistEndret() ?: LocalDateTime.MIN }
            )

            bruker.fargekategori = fargekategori?.verdi()
            bruker.fargekategori_enhetId = fargekategori?.enhetId()
        }
    }
}
