package no.nav.pto.veilarbportefolje.api.beslutteroversikt

import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModellMapper
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService
import org.springframework.stereotype.Service

@Service
class BeslutteroversiktService(
    private val opensearchService: OpensearchService
) {

    fun hentEtiketter(fnrListe: List<String>): List<EtiketterForBruker> =
        opensearchService.hentBrukereMedFnr(fnrListe)
            .map { (fnr, bruker) ->
                EtiketterForBruker(
                    fnr = fnr,
                    etiketter = EtiketterBeslutteroversikt(
                        erDoed = bruker.er_doed,
                        egenAnsatt = bruker.egen_ansatt,
                        adressebeskyttelse = PortefoljebrukerFrontendModellMapper.mapAdressebeskyttelse(
                            bruker
                        ),
                        kandidatForUtmelding = PortefoljebrukerFrontendModellMapper.erKandidatForUtmelding(bruker)
                    )
                )
            }
}
