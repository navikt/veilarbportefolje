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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlBrukerdataKafkaService.hentAktorider;
import static no.nav.pto.veilarbportefolje.persononinfo.PdlService.hentAktivFnr;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class BarnUnder18AarKafkaTest {

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
    private final String pdlDokumentAsString = readFileAsJsonString("/pdl_dokument.json", getClass());
    private final String pdlDokumentBarn1MedDiskresjonskodeAsString = readFileAsJsonString("/pdl_dokument_barn1_med_diskresjonskode.json", getClass());
    private final String pdlIdentResponsFraFil = readFileAsJsonString("/identer_pdl.json", getClass());
    private final String pdlPersonResponsFraFil = readFileAsJsonString("/person_pdl.json", getClass());

    private final String pdlPersonMed3BarnResponsFraFil = readFileAsJsonString("/person_pdl_3barn.json", getClass());
    private final String pdlPersonMed2BarnResponsFraFil = readFileAsJsonString("/person_pdl_2barn.json", getClass());
    private final String pdlPersonBarn1ResponsFraFil = readFileAsJsonString("/person_barn_pdl.json", getClass());
    private final String pdlPersonBarn2ResponsFraFil = readFileAsJsonString("/person_barn2_pdl.json", getClass());
    private final String pdlPersonBarn3ResponsFraFil = readFileAsJsonString("/person_barn3_pdl.json", getClass());
    private final JdbcTemplate db;


    public BarnUnder18AarKafkaTest() {
        this.db = SingletonPostgresContainer.init().createJdbcTemplate();
        barnUnder18AarRepository = new BarnUnder18AarRepository(db,db);
        barnUnder18AarService = new BarnUnder18AarService(this.barnUnder18AarRepository);
        pdlIdentRepository = new PdlIdentRepository(db);
        pdlPersonRepository = new PdlPersonRepository(db, db);
        oppfolgingsbrukerRepositoryV3 = new OppfolgingsbrukerRepositoryV3(db, null);
        oppfolgingRepositoryV2 = new OppfolgingRepositoryV2(db);
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
                                .withBody(pdlPersonBarn1ResponsFraFil))
                        .willSetStateTo("hent barn 2")
        );
   /*
        server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonBarn1ResponsFraFil))
                .whenScenarioStateIs("hent person")
                .willSetStateTo("hent barn 1")
        );
*/
      /*  server.stubFor(post(anyUrl())
                .inScenario("PDL test")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(pdlPersonBarn1ResponsFraFil))
                .whenScenarioStateIs("hent barn 1")
                .willSetStateTo("hent barn 2")
        );
*/
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
                this.pdlPersonRepository,
                new BrukerServiceV2(this.pdlIdentRepository, this.oppfolgingsbrukerRepositoryV3, this.oppfolgingRepositoryV2),
                this.barnUnder18AarService,
                opensearchIndexer,
                opensearchIndexerV2
                );
    }

    @Test
    public void testHentBarnUnder18Aar() throws JsonProcessingException {
        List<Fnr> foreldre = List.of(randomFnr(), randomFnr());
        List<Fnr> barn = List.of(randomFnr(), randomFnr(), randomFnr(), randomFnr(), randomFnr());

        //PdlDokument pdlDokument = PdlDokument.builder().build();


        var pdlDokForelder = mapper.readValue(pdlDokumentAsString, PdlDokument.class);
        var pdlDokBarn1MedDiskresjonskode = mapper.readValue(pdlDokumentBarn1MedDiskresjonskodeAsString, PdlDokument.class);

        //upsert identer for brukeren i pdlIdentRepository slik at den er under oppfølging
        List<PDLIdent> pdlIdenter = pdlDokForelder.getHentIdenter().getIdenter();
        List<AktorId> aktorIder = hentAktorider(pdlIdenter);
        Fnr fnrForelder = hentAktivFnr(pdlIdenter);
        aktorIder.forEach(aktorId -> {
            oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now());
        });

        pdlIdentRepository.upsertIdenter(pdlDokForelder.getHentIdenter().getIdenter());

        pdlBrukerdataKafkaService.behandleKafkaMeldingLogikk(pdlDokForelder);
        String diskresjonskode_barn1 = barnUnder18AarService.hentBarnUnder18Aar(List.of(fnrForelder)).get(fnrForelder).get(0).getDiskresjonskode();
        // hente ut diskresjonskode for barnet
        // sende inn pdlDokBarn her med samme barn men update
        pdlBrukerdataKafkaService.behandleKafkaMeldingLogikk(pdlDokBarn1MedDiskresjonskode);
        String diskresjonskode_barn1_etter_update = barnUnder18AarService.hentBarnUnder18Aar(List.of(fnrForelder)).get(fnrForelder).get(0).getDiskresjonskode();
        Assertions.assertTrue(diskresjonskode_barn1 == null);
        Assertions.assertTrue(Objects.equals(diskresjonskode_barn1_etter_update, "7"));
        // Sjekke at barnet blir endret (feks endret diskresjonskode)
        // Gjøre et av barnene død (pdlDokBarn2)
        // Endre fnr på et av barnene?





    }
}
