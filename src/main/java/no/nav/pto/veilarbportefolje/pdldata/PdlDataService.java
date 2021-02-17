package no.nav.pto.veilarbportefolje.pdldata;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import no.nav.common.client.pdl.AktorOppslagClient;
import no.nav.common.client.pdl.PdlClient;
import no.nav.common.featuretoggle.UnleashService;
import no.nav.common.json.JsonUtils;
import no.nav.common.rest.client.RestClient;
import no.nav.common.rest.client.RestUtils;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.common.utils.FileUtils;
import no.nav.pto.veilarbportefolje.auth.AuthUtils;
import no.nav.pto.veilarbportefolje.config.FeatureToggle;
import no.nav.pto.veilarbportefolje.database.BrukerRepository;
import no.nav.pto.veilarbportefolje.domene.AktorClient;
import no.nav.pto.veilarbportefolje.elastic.domene.OppfolgingsBruker;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static no.nav.common.utils.UrlUtils.joinPaths;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Service
@Slf4j
public class PdlDataService {
    private final PdlRepository pdlRepository;
    private final PdlClient pdlClient;
    private final BrukerRepository brukerRepository;
    private final String hentPersonQuery;
    private final UnleashService unleashService;
    private final OkHttpClient client;
    private final AktorClient aktorClient;

    @Autowired
    public PdlDataService(PdlRepository pdlRepository, PdlClient pdlClient, BrukerRepository brukerRepository, UnleashService unleashService, AktorClient aktorClient) {
        this.brukerRepository = brukerRepository;
        this.pdlRepository = pdlRepository;
        this.pdlClient = pdlClient;
        this.unleashService = unleashService;
        this.aktorClient = aktorClient;
        this.hentPersonQuery = FileUtils.getResourceFileAsString("graphql/hentPersonFodselsdag.gql");

        this.client = RestClient.baseClient();
    }

    public void lastInnPdlData(AktorId aktorId) {
        String fodselsdag;
        if (erPdlPa(unleashService)) {
            fodselsdag = hentFodseldagFraPdl(aktorId);
        } else {
            Fnr fnr = aktorClient.hentFnr(aktorId);
            fodselsdag = hentFodseldagFraVeilarbPeron(fnr);
        }
        pdlRepository.upsert(aktorId, DateUtils.dateToTime(fodselsdag));
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

    @SneakyThrows
    private String hentFodseldagFraPdl(AktorId aktorId) {
        GqlRequest<PdlPersonVariables.HentFodselsdag> request = new GqlRequest<>(hentPersonQuery, new PdlPersonVariables.HentFodselsdag(aktorId.get()));
        PdlDto respons = parseGqlJsonResponse(pdlClient.rawRequest(JsonUtils.toJson(request)));
        return getFodselsdato(respons);
    }

    private static PdlDto parseGqlJsonResponse(String gqlJsonResponse) throws JsonProcessingException {
        if (gqlJsonResponse == null) {
            return null;
        }
        ObjectMapper mapper = JsonUtils.getMapper();
        JsonNode gqlResponseNode = mapper.readTree(gqlJsonResponse);
        JsonNode errorsNode = gqlResponseNode.get("errors");

        if (errorsNode != null) {
            log.error("Kall mot PDL feilet:\n" + errorsNode.toPrettyString());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return mapper.treeToValue(gqlResponseNode.get("data"), PdlDto.class);
    }

    private static String getFodselsdato(PdlDto person) {
        if (person == null) {
            return null;
        }

        Optional<PdlDto.Foedsel> fodsel = person.getHentPerson().foedsel.stream().findFirst();
        return fodsel.map(PdlDto.Foedsel::getFoedselsdato).orElse(null);
    }

    @SneakyThrows
    private String hentFodseldagFraVeilarbPeron(Fnr fnr) {
        Request request = new Request.Builder()
                .url(joinPaths("/veilarbperson/api/person/", fnr.get()))
                .header(ACCEPT, APPLICATION_JSON_VALUE)
                .header(AUTHORIZATION, AuthUtils.getInnloggetBrukerToken()) // TODO: Denne vil ikke være tilgjengelig
                .build();

        try (Response response = client.newCall(request).execute()) {
            RestUtils.throwIfNotSuccessful(response);
            return RestUtils.parseJsonResponseOrThrow(response, PersonData.class).getFodselsdato();
        }
    }

    private boolean erPdlPa(UnleashService unleashService) {
        return unleashService.isEnabled(FeatureToggle.PDL);
    }

}
