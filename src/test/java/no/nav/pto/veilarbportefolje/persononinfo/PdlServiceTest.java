package no.nav.pto.veilarbportefolje.persononinfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlIdentResponse;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLPerson;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlService.hentAktivFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.assertj.core.api.Assertions.assertThat;

public class PdlServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String pdlIdentResponsFraFil = readFileAsJsonString("/identer_pdl.json", getClass());
    private final String pdlPersonRespnsFraFil = readFileAsJsonString("/person_pdl.json", getClass());
    private final JdbcTemplate db;
    private final PdlPersonRepository pdlPersonRepository;
    private PdlService pdlService;

    public PdlServiceTest() {
        this.db = SingletonPostgresContainer.init().createJdbcTemplate();
        pdlPersonRepository = new PdlPersonRepository(db);
    }

    @BeforeEach
    public void setup() {
        WireMockServer server = new WireMockServer();
        db.update("truncate bruker_identer");
        server.stubFor(
                post(anyUrl())
                        .inScenario("PDL test")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody(pdlIdentResponsFraFil))
                        .willSetStateTo("hent person")
        );

        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonRespnsFraFil))
                .whenScenarioStateIs("hent person")
        );
        server.start();

        this.pdlService = new PdlService(
                new PdlIdentRepository(db),
                new PdlPersonRepository(db),
                new PdlPortefoljeClient(new PdlClientImpl("http://localhost:" + server.port(), () -> "SYSTEM"))
        );
    }

    @Test
    @SneakyThrows
    public void hentOgLagrePdlData() {
        var identerFraFil = mapper.readValue(pdlIdentResponsFraFil, PdlIdentResponse.class)
                .getData()
                .getHentIdenter()
                .getIdenter();

        pdlService.hentOgLagrePdlData(randomAktorId());
        List<PDLIdent> identerFraPostgres = db.queryForList("select * from bruker_identer")
                .stream()
                .map(PdlIdentRepository::mapTilident)
                .toList();
        Fnr fnr = hentAktivFnr(identerFraFil);
        PDLPerson pdlPerson = pdlPersonRepository.hentPerson(fnr);

        assertThat(identerFraPostgres).containsExactlyInAnyOrderElementsOf(identerFraFil);
        assertThat(pdlPerson.getFornavn()).isEqualTo("TREIG");
    }
}
