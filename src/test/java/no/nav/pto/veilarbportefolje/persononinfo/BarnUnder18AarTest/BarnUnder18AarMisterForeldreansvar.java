package no.nav.pto.veilarbportefolje.persononinfo.BarnUnder18AarTest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.*;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlDokument;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarRepository;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlBrukerdataKafkaService.hentAktorider;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlService.hentAktivFnr;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.mockito.Mockito.mock;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class BarnUnder18AarMisterForeldreansvar {

    private final ObjectMapper mapper = new ObjectMapper();
    private BarnUnder18AarRepository barnUnder18AarRepository;

    private BarnUnder18AarService barnUnder18AarService;

    private PdlBrukerdataKafkaService pdlBrukerdataKafkaService;

    private PdlIdentRepository pdlIdentRepository;

    private PdlPersonRepository pdlPersonRepository;

    private OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;

    private OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    @MockBean
    private OpensearchIndexer opensearchIndexer;
    @MockBean
    private OpensearchIndexerV2 opensearchIndexerV2;
    private final String pdlDokumentAsString = readFileAsJsonString("/PDL_Files/pdl_dokument.json", getClass());
    private final String pdlDokumentIngenBarn = readFileAsJsonString("/PDL_Files/pdl_dokument_ingen_barn.json", getClass());
    private final String pdlPersonBarn1ResponsFraFil = readFileAsJsonString("/PDL_Files/person_barn_pdl.json", getClass());
    private final String pdlPersonBarn2ResponsFraFil = readFileAsJsonString("/PDL_Files/person_barn2_pdl.json", getClass());
    private final String pdlPersonBarn3ResponsFraFil = readFileAsJsonString("/PDL_Files/person_barn3_pdl.json", getClass());
    private final JdbcTemplate db;

    private WireMockServer server = new WireMockServer();


    public BarnUnder18AarMisterForeldreansvar() {
        this.db = SingletonPostgresContainer.init().createJdbcTemplate();
        barnUnder18AarRepository = new BarnUnder18AarRepository(db, db);
        barnUnder18AarService = new BarnUnder18AarService(this.barnUnder18AarRepository, mock(PdlPortefoljeClient.class));
        pdlIdentRepository = new PdlIdentRepository(db);
        pdlPersonRepository = new PdlPersonRepository(db, db);
        oppfolgingsbrukerRepositoryV3 = new OppfolgingsbrukerRepositoryV3(db, null);
        oppfolgingRepositoryV2 = new OppfolgingRepositoryV2(db);
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
                                .withBody(pdlPersonBarn1ResponsFraFil))
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
        );

        server.start();


        this.pdlBrukerdataKafkaService = new PdlBrukerdataKafkaService(new PdlService(
                this.pdlIdentRepository,
                this.pdlPersonRepository,
                this.barnUnder18AarService,
                new PdlPortefoljeClient(new PdlClientImpl("http://localhost:" + server.port(), () -> "SYSTEM_TOKEN"))
        ), this.pdlIdentRepository,
                new BrukerServiceV2(this.pdlIdentRepository, this.oppfolgingsbrukerRepositoryV3, this.oppfolgingRepositoryV2),
                this.barnUnder18AarService,
                opensearchIndexer,
                opensearchIndexerV2
        );
    }


    @AfterEach
    public void stopServer() {
        server.stop();
    }

    @Test
    public void testHentBarnUnder18Aar() throws JsonProcessingException {
        var pdlDokForelder = mapper.readValue(pdlDokumentAsString, PdlDokument.class);
        var pdlDokForelderIngenBarn = mapper.readValue(pdlDokumentIngenBarn, PdlDokument.class);

        List<PDLIdent> pdlIdenter = pdlDokForelder.getHentIdenter().getIdenter();
        List<AktorId> aktorIder = hentAktorider(pdlIdenter);
        Fnr fnrForelder = hentAktivFnr(pdlIdenter);
        aktorIder.forEach(aktorId -> {
            oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());
        });

        pdlIdentRepository.upsertIdenter(pdlDokForelder.getHentIdenter().getIdenter());

        pdlBrukerdataKafkaService.behandleKafkaMeldingLogikk(pdlDokForelder);
        List<Fnr> barnFoer  = barnUnder18AarService.hentBarnFnrsForForeldre(List.of(fnrForelder));
        pdlBrukerdataKafkaService.behandleKafkaMeldingLogikk(pdlDokForelderIngenBarn);
        List<Fnr> barnEtter = barnUnder18AarService.hentBarnFnrsForForeldre(List.of(fnrForelder));
        Assertions.assertTrue(barnFoer.size() == 3);
        Assertions.assertTrue(barnEtter.isEmpty());
    }
}
