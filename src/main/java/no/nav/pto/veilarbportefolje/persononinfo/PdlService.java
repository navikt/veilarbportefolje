package no.nav.pto.veilarbportefolje.persononinfo;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.common.types.identer.AktorId;
import no.nav.common.utils.FileUtils;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlFodselsRespons;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlPersonVariables;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdlService {
    private final PdlRepository pdlRepository;
    private final PdlClient pdlClient;

    private final String hentPersonQuery = FileUtils.getResourceFileAsString("graphql/hentPerson.gql");

    @SneakyThrows
    public void lastInnPdlData(AktorId aktorId) {
        hentHistoriskeIdenter(aktorId);
        String fodselsdag = hentFodseldagFraPdl(aktorId);
        pdlRepository.upsertFodselsdag(aktorId, LocalDate.parse(fodselsdag));
    }

    private String hentHistoriskeIdenter(AktorId aktorId) {
        return null;
    }

    public void slettPdlData(AktorId aktorId) {
        pdlRepository.slettPdlData(aktorId);
    }

    private String hentFodseldagFraPdl(AktorId aktorId) throws RuntimeException {
        GraphqlRequest<PdlPersonVariables.HentFodselsdag> request = new GraphqlRequest<>(hentPersonQuery, new PdlPersonVariables.HentFodselsdag(aktorId.get()));
        PdlFodselsRespons respons = pdlClient.request(request, PdlFodselsRespons.class);
        if (hasErrors(respons)) {
            throw new RuntimeException();
        }

        return respons.getData()
                .getHentPerson()
                .getFoedsel()
                .stream()
                .findFirst()
                .map(PdlFodselsRespons.HentFodselsResponseData.HentPersonDataResponsData.Foedsel::getFoedselsdato)
                .orElseThrow();
    }

    public static <T> boolean hasErrors(GraphqlResponse<T> response) throws RuntimeException {
        if (response == null || response.getErrors() == null) {
            return true;
        }
        return !response.getErrors().isEmpty();
    }
}
