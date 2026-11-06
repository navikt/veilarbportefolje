package no.nav.pto.veilarbportefolje.beslutteroversikt

import no.nav.pto.veilarbportefolje.auth.AuthService
import no.nav.pto.veilarbportefolje.auth.AuthUtils
import no.nav.pto.veilarbportefolje.domene.frontendmodell.PortefoljebrukerFrontendModellMapper
import no.nav.pto.veilarbportefolje.opensearch.OpensearchService
import org.springframework.stereotype.Service

@Service
class BeslutteroversiktService(
    private val opensearchService: OpensearchService,
    private val authService: AuthService
) {

    fun hentEtiketter(fnrListe: List<String>): List<EtiketterForBruker> {
        val veilederId = AuthUtils.getInnloggetVeilederIdent().toString()
        val brukere = opensearchService.hentBrukereMedFnr(fnrListe)

        val enheterMedTilgang = brukere.mapNotNull { it.enhet_id }
            .distinct()
            .filter { authService.harVeilederTilgangTilEnhet(veilederId, it) }
            .toSet()

        return brukere
            .filter { it.enhet_id in enheterMedTilgang }
            .mapNotNull { bruker ->
                val fnr = bruker.fnr ?: return@mapNotNull null

                EtiketterForBruker(
                    fnr = fnr,
                    etiketter = EtiketterBeslutteroversikt(
                        erDoed = bruker.er_doed,
                        egenAnsatt = bruker.egen_ansatt,
                        diskresjonskodeFortrolig = PortefoljebrukerFrontendModellMapper.mapDiskresjonskodeFortrolig(
                            bruker
                        ),
                        kandidatForUtmelding = PortefoljebrukerFrontendModellMapper.erKandidatForUtmelding(bruker)
                    )
                )
            }
    }
}
