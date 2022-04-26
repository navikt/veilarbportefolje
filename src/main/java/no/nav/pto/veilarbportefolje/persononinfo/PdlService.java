package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.common.types.identer.AktorId;
import no.nav.common.utils.FileUtils;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlIdentRespons;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlIdentVariabel;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdlService {
    private final PdlRepository pdlRepository;
    private final PdlClient pdlClient;

    private static final String hentIdenterQuery = FileUtils.getResourceFileAsString("graphql/hentIdenter.gql");

    public void hentOgLagreIdenter(AktorId aktorId) {
        log.info("Oppdaterer ident mapping for aktor: {}", aktorId);

        List<PDLIdent> idents = hentIdenterFraPdl(aktorId);
        pdlRepository.upsertIdenter(idents);
    }

    public void slettPdlData(AktorId aktorId) {
        pdlRepository.slettLokalIdentlagringHvisIkkeUnderOppfolging(aktorId);
    }

    private List<PDLIdent> hentIdenterFraPdl(AktorId aktorId) throws RuntimeException {
        GraphqlRequest<PdlIdentVariabel> request = new GraphqlRequest<>(hentIdenterQuery, new PdlIdentVariabel(aktorId.get()));
        PdlIdentRespons respons = pdlClient.request(request, PdlIdentRespons.class);
        if (hasErrors(respons)) {
            throw new RuntimeException("Kunne ikke hente identer fra PDL");
        }

        return respons.getData()
                .getHentIdenter()
                .getIdenter();
    }

    private static <T> boolean hasErrors(GraphqlResponse<T> response) {
        return response == null || (response.getErrors() != null && !response.getErrors().isEmpty());
    }

    public int hentIdenterSomIkkeErMappet() {
        return pdlRepository.hentIdenterSomIkkeErMappet().size();
    }

    public void mapManglendeIdenter() {
        List<AktorId> aktorIder = pdlRepository.hentIdenterSomIkkeErMappet();

        aktorIder.forEach(aktorId -> {
            try {
                hentOgLagreIdenter(aktorId);
            } catch (Exception e) {
                log.info("feil under innlastning av ident", e);
            }
        });
    }

}
