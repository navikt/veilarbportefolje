package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.FileUtils;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlIdentResponse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlIdentVariabel;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonResponse;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import org.springframework.stereotype.Service;

import java.util.List;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson.genererFraApiRespons;

@Service
@RequiredArgsConstructor
public class PdlPortefoljeClient {
    private final PdlClient pdlClient;
    private static final String hentIdenterQuery = FileUtils.getResourceFileAsString("graphql/hentIdenter.gql");
    private static final String hentPersonQuery = FileUtils.getResourceFileAsString("graphql/hentPerson.gql");

    public PDLPerson hentBrukerDataFraPdl(Fnr fnr) throws RuntimeException {
        GraphqlRequest<PdlIdentVariabel> request = new GraphqlRequest<>(hentPersonQuery, new PdlIdentVariabel(fnr.get()));
        PdlPersonResponse respons = pdlClient.request(request, PdlPersonResponse.class);
        if (hasErrors(respons)) {
            throw new RuntimeException("Kunne ikke hente identer fra PDL");
        }

        return genererFraApiRespons(respons.getData().getHentPerson());
    }

    public List<PDLIdent> hentIdenterFraPdl(AktorId aktorId) throws RuntimeException {
        GraphqlRequest<PdlIdentVariabel> request = new GraphqlRequest<>(hentIdenterQuery, new PdlIdentVariabel(aktorId.get()));
        PdlIdentResponse respons = pdlClient.request(request, PdlIdentResponse.class);
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
}
