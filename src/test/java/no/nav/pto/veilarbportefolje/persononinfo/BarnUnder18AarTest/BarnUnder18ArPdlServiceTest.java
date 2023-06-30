package no.nav.pto.veilarbportefolje.persononinfo.BarnUnder18AarTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import lombok.SneakyThrows;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.persononinfo.PdlIdentRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPersonRepository;
import no.nav.pto.veilarbportefolje.persononinfo.PdlPortefoljeClient;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlBarnResponse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlIdentResponse;
import no.nav.pto.veilarbportefolje.persononinfo.PdlService;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarData;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarRepository;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.util.DateUtils;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlService.hentAktivFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.assertj.core.api.Assertions.assertThat;

public class BarnUnder18ArPdlServiceTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private final String pdlIdentResponsFraFil = readFileAsJsonString("/PDL_Files/identer_pdl.json", getClass());
    private final String pdlPersonMed3BarnResponsFraFil = readFileAsJsonString("/PDL_Files/person_pdl_3barn.json", getClass());
    private final String pdlPersonMed2BarnResponsFraFil = readFileAsJsonString("/PDL_Files/person_pdl_2barn.json", getClass());
    private final String pdlPersonBarn1ResponsFraFil = readFileAsJsonString("/PDL_Files/person_barn_pdl.json", getClass());
    private final String pdlPersonBarn2ResponsFraFil = readFileAsJsonString("/PDL_Files/person_barn2_pdl.json", getClass());
    private final String pdlPersonBarn3ResponsFraFil = readFileAsJsonString("/PDL_Files/person_barn3_pdl.json", getClass());
    private final JdbcTemplate db;

    private BarnUnder18AarService barnUnder18AarService;
    private PdlService pdlService;

    private WireMockServer server = new WireMockServer();

    public BarnUnder18ArPdlServiceTest() {
        this.db = SingletonPostgresContainer.init().createJdbcTemplate();
    }

    @BeforeEach
    public void setup() {
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
                        .withBody(pdlPersonMed3BarnResponsFraFil))
                .whenScenarioStateIs("hent person")
                .willSetStateTo("hent barn 1")
        );

        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonBarn1ResponsFraFil))
                .whenScenarioStateIs("hent barn 1")
                .willSetStateTo("hent barn 2")
        );

        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonBarn2ResponsFraFil))
                .whenScenarioStateIs("hent barn 2")
                .willSetStateTo("hent barn 3")
        );

        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonBarn3ResponsFraFil))
                .whenScenarioStateIs("hent barn 3")
                .willSetStateTo("hent identer")
        );

        server.stubFor(
                post(anyUrl())
                        .inScenario("PDL test")
                        .willReturn(aResponse()
                                .withStatus(200)
                                .withBody(pdlIdentResponsFraFil))
                        .whenScenarioStateIs("hent identer")
                        .willSetStateTo("hent person med 2 barn")
        );
        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonMed2BarnResponsFraFil))
                .whenScenarioStateIs("hent person med 2 barn")
                .willSetStateTo("hent barn 1 igjen")
        );
        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonBarn1ResponsFraFil))
                .whenScenarioStateIs("hent barn 1 igjen")
                .willSetStateTo("hent barn 3 igjen")
        );

        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonBarn3ResponsFraFil))
                .whenScenarioStateIs("hent barn 3 igjen")
        );

        server.start();

        PdlPortefoljeClient pdlPortefoljeClient = new PdlPortefoljeClient(new PdlClientImpl("http://localhost:" + server.port(), () -> "SYSTEM_TOKEN"));

        this.barnUnder18AarService = new BarnUnder18AarService(new BarnUnder18AarRepository(db, db), pdlPortefoljeClient);
        this.pdlService = new PdlService(
                new PdlIdentRepository(db),
                new PdlPersonRepository(db, db),
                this.barnUnder18AarService,
                pdlPortefoljeClient);
    }

    @AfterEach
    public void stopServer(){
        server.stop();
    }
    @Test
    @SneakyThrows
    public void sjekkAtLagretBarnIkkeFjernes() {
        var identerFraFil = mapper.readValue(pdlIdentResponsFraFil, PdlIdentResponse.class)
                .getData()
                .getHentIdenter()
                .getIdenter();

        AktorId aktorId = randomAktorId();
        pdlService.hentOgLagrePdlData(aktorId);
        List<PDLIdent> identerFraPostgres = db.queryForList("select * from bruker_identer")
                .stream()
                .map(PdlIdentRepository::mapTilident)
                .toList();
        Fnr fnr = hentAktivFnr(identerFraFil);

        List<Fnr> foreldreansvar1 = barnUnder18AarService.hentBarnFnrsForForeldre(List.of(fnr));
        assertThat(foreldreansvar1.size()).isEqualTo(3);
        pdlService.hentOgLagrePdlData(aktorId);

        List<Fnr> foreldreansvar2 = barnUnder18AarService.hentBarnFnrsForForeldre(List.of(fnr));
        List<BarnUnder18AarData> barnFraRepository2 = barnUnder18AarService.hentBarnUnder18Aar(List.of(fnr)).get(fnr);
        assertThat(foreldreansvar2.size()).isEqualTo(2);

        assertThat(identerFraPostgres).containsExactlyInAnyOrderElementsOf(identerFraFil);
        assertThat(foreldreansvar1.size()).isEqualTo(3);
        assertThat(barnFraRepository2.get(0).getAlder()).isEqualTo(DateUtils.alderFraFodselsdato(DateUtils.toLocalDateOrNull(hentFodselsdatoBarn1())));
        assertThat(barnFraRepository2.get(1).getAlder()).isEqualTo(DateUtils.alderFraFodselsdato(DateUtils.toLocalDateOrNull(hentFodselsdatoBarn3())));
    }

    public String hentFodselsdatoBarn1() throws JsonProcessingException {
        var fdato = mapper.readValue(pdlPersonBarn1ResponsFraFil, PdlBarnResponse.class)
                .getData()
                .getHentPerson()
                .getFoedsel()
                .get(0)
                .getFoedselsdato();
        return fdato;
    }

    public String hentFodselsdatoBarn2() throws JsonProcessingException {
        var fdato = mapper.readValue(pdlPersonBarn2ResponsFraFil, PdlBarnResponse.class)
                .getData()
                .getHentPerson()
                .getFoedsel()
                .get(0)
                .getFoedselsdato();
        return fdato;
    }

    public String hentFodselsdatoBarn3() throws JsonProcessingException {
        var fdato = mapper.readValue(pdlPersonBarn3ResponsFraFil, PdlBarnResponse.class)
                .getData()
                .getHentPerson()
                .getFoedsel()
                .get(0)
                .getFoedselsdato();
        return fdato;
    }
}



