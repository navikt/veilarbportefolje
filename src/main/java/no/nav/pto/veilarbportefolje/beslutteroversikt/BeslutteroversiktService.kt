package no.nav.pto.veilarbportefolje.beslutteroversikt

import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModellMapper
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService
import org.springframework.stereotype.Service

@Service
class BeslutteroversiktService(
    private val opensearchService: OpensearchService
) {

    fun hentEtiketter(fnrListe: List<String>): List<EtiketterForBruker> {
        val brukere = opensearchService.hentBrukereMedFnr(fnrListe)

        return brukere.mapNotNull { bruker ->
            val fnr = bruker.fnr ?: return@mapNotNull null

            EtiketterForBruker(
                fnr = fnr,
                etiketter = EtiketterBeslutteroversikt(
                    erDoed = bruker.er_doed,
                    diskresjonskodeFortrolig = PortefoljebrukerFrontendModellMapper.mapDiskresjonskodeFortrolig(bruker),
                    kandidatForUtmelding = PortefoljebrukerFrontendModellMapper.erKandidatForUtmelding(bruker)
                )
            )
        }
    }
}
