package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.common.json.JsonUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.FileUtils;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.*;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPersonBarn;
import no.nav.pto.veilarbportefolje.util.SecureLog;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson.genererFraApiRespons;

@Service
@RequiredArgsConstructor
public class PdlPortefoljeClient {
    private final PdlClient pdlClient;
    private static final String hentIdenterQuery = FileUtils.getResourceFileAsString("graphql/hentIdenter.gql");
    private static final String hentPersonQuery = FileUtils.getResourceFileAsString("graphql/hentPerson.gql");

    private static final String hentPersonBarnQuery = FileUtils.getResourceFileAsString("graphql/hentPersonBarn.gql");
    private static final String hentPersonBarnBolkQuery = FileUtils.getResourceFileAsString("graphql/hentPersonBarnBolk.gql");

    public PDLPerson hentBrukerDataFraPdl(Fnr fnr) throws RuntimeException {
        GraphqlRequest<PdlIdentVariabel> request = new GraphqlRequest<>(hentPersonQuery, new PdlIdentVariabel(fnr.get(), false));
        PdlPersonResponse respons = pdlClient.request(request, PdlPersonResponse.class);
        if (hasErrors(respons)) {
            throw new RuntimeException("Kunne ikke hente identer fra PDL");
        }

        return genererFraApiRespons(respons.getData().getHentPerson());
    }

    public PDLPersonBarn hentBrukerBarnDataFraPdl(Fnr fnr) throws RuntimeException {
        GraphqlRequest<PdlIdentVariabel> request = new GraphqlRequest<>(hentPersonBarnQuery, new PdlIdentVariabel(fnr.get(), false));
        PdlBarnResponse respons = pdlClient.request(request, PdlBarnResponse.class);
        if (hasErrors(respons)) {
            SecureLog.secureLog.info("Error in hentBrukerDataBarnFraPdl, request: {}, response: {}", request, respons.toString());
            throw new RuntimeException("Kunne ikke hente data om barn fra PDL");

        }

        return PDLPersonBarn.genererFraApiRespons(respons.getData().getHentPerson());
    }

    public List<PDLIdent> hentIdenterFraPdl(AktorId aktorId) throws RuntimeException {
        GraphqlRequest<PdlIdentVariabel> request = new GraphqlRequest<>(hentIdenterQuery, new PdlIdentVariabel(aktorId.get(), true));
        PdlIdentResponse respons = pdlClient.request(request, PdlIdentResponse.class);
        if (hasErrors(respons)) {
            throw new RuntimeException("Kunne ikke hente identer fra PDL");
        }

        System.out.println("SE HER:");
        System.out.println("AktørId:" + aktorId.get());
        System.out.println("Respons:" + JsonUtils.toJson(respons));

        return respons.getData()
                .getHentIdenter()
                .getIdenter();
    }

    public Map<Fnr, PDLPersonBarn> hentBrukerBarnDataBolkFraPdl(List<Fnr> barnIdenter) {
        if (barnIdenter == null || barnIdenter.isEmpty()){
            return Collections.emptyMap();
        }

        List<String> barnIdenterStr = barnIdenter.stream().map(Fnr::get).toList();

        GraphqlRequest<PdlIdenterVariabel> request = new GraphqlRequest<>(hentPersonBarnBolkQuery, new PdlIdenterVariabel(barnIdenterStr));
        PDLPersonBarnBolk respons = pdlClient.request(request, PDLPersonBarnBolk.class);
        if (hasErrors(respons)) {
            throw new RuntimeException("Kunne ikke hente bolk barn data ");
        }

        return PDLPersonBarn.genererFraApiRespons(respons.getData());
    }

    private static <T> boolean hasErrors(GraphqlResponse<T> response) {
        return response == null || (response.getErrors() != null && !response.getErrors().isEmpty());
    }
}
