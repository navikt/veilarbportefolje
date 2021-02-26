package no.nav.pto.veilarbportefolje.pdldata;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.client.utils.graphql.GraphqlRequest;
import no.nav.common.client.utils.graphql.GraphqlResponse;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.rest.client.RestClient;
import no.nav.common.types.identer.AktorId;
import no.nav.common.utils.FileUtils;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class PdlDataService {
    private final PdlRepository pdlRepository;
    private final PdlClient pdlClient;
    private final BrukerRepository brukerRepository;
    private final String hentPersonQuery;
    private final UnleashService unleashService;

    @Autowired
    public PdlDataService(PdlRepository pdlRepository, PdlClient pdlClient, BrukerRepository brukerRepository, UnleashService unleashService) {
        this.brukerRepository = brukerRepository;
        this.pdlRepository = pdlRepository;
        this.pdlClient = pdlClient;
        this.unleashService = unleashService;
        this.hentPersonQuery = FileUtils.getResourceFileAsString("graphql/hentPersonFodselsdag.gql");

    }

    public void lastInnPdlData(AktorId aktorId) {
        try {
            String fodselsdag;
            if (erPdlPa(unleashService)) {
                fodselsdag = hentFodseldagFraPdl(aktorId);
            } else {
                fodselsdag = hentFodseldagFraTps(aktorId);
            }

            pdlRepository.upsert(aktorId, DateUtils.getLocalDateFromSimpleISODate(fodselsdag));
        } catch (RuntimeException exception) {
            System.out.println("fix error"); // TODO: legg inn i feiltabell;
        }
    }

    public void slettPdlData(AktorId aktorId) {
        pdlRepository.slettPdlData(aktorId);
    }

    public void lastInnDataFraDbLinkTilPdlDataTabell() {
        List<OppfolgingsBruker> brukere = brukerRepository.hentAlleBrukereUnderOppfolging();
        log.info("lastInnDataFraDbLinkTilPdlDataTabell: Hentet {} oppfølgingsbrukere fra databasen", brukere.size());
        pdlRepository.saveBatch(brukere);
        log.info("lastInnDataFraDbLinkTilPdlDataTabell: fullført");
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
                .map(fodselsdag -> fodselsdag.getFoedselsdato())
                .orElseThrow();
    }


    @SneakyThrows
    private String hentFodseldagFraTps(AktorId aktorId) {
        return null;
    }

    private boolean erPdlPa(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.PDL);
    }

    public static <T> boolean hasErrors(GraphqlResponse<T> response) throws RuntimeException {
        if (response == null || response.getErrors() == null) {
            return true;
        }
        return !response.getErrors().isEmpty();
    }
}
