package no.nav.pto.veilarbportefolje.persononinfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.common.client.pdl.PdlClientImpl;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlDokument;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarRepository;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.service.UnleashService;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class PdlBrukerdataKafkaServiceTest {

    private final PdlDokument randomPdlDokument;

    private static BarnUnder18AarRepository barnUnder18AarRepository;
    private static PdlBrukerdataKafkaService pdlBrukerdataKafkaService;

    private static PdlIdentRepository pdlIdentRepository;

    private static PdlPersonRepository pdlPersonRepository;

    private static OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3;

    private static OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    @MockBean
    private static OpensearchIndexer opensearchIndexer;
    @MockBean
    private static OpensearchIndexerV2 opensearchIndexerV2;

    @MockBean
    private static UnleashService unleashService;

    private static final WireMockServer server = new WireMockServer();

    private static JdbcTemplate db;

    public PdlBrukerdataKafkaServiceTest() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String pdlDokumentAsString = readFileAsJsonString("/PDL_Files/pdl_dokument.json", getClass());
        randomPdlDokument = mapper.readValue(pdlDokumentAsString, PdlDokument.class);
    }

    @BeforeAll
    public static void setup() {
        db = SingletonPostgresContainer.init().createJdbcTemplate();
        barnUnder18AarRepository = new BarnUnder18AarRepository(db, db);
        pdlIdentRepository = new PdlIdentRepository(db);
        pdlPersonRepository = new PdlPersonRepository(db, db);
        oppfolgingsbrukerRepositoryV3 = new OppfolgingsbrukerRepositoryV3(db, null);
        oppfolgingRepositoryV2 = new OppfolgingRepositoryV2(db);

        db.update("truncate bruker_data CASCADE");
        db.update("truncate bruker_data_barn CASCADE");
        db.update("truncate foreldreansvar");
        server.start();

        PdlPortefoljeClient pdlPortefoljeClient = new PdlPortefoljeClient(new PdlClientImpl("http://localhost:" + server.port(), () -> "SYSTEM_TOKEN"));
        BarnUnder18AarService barnUnder18AarService = new BarnUnder18AarService(barnUnder18AarRepository, pdlPortefoljeClient);

        pdlBrukerdataKafkaService = new PdlBrukerdataKafkaService(new PdlService(
                pdlIdentRepository,
                pdlPersonRepository,
                barnUnder18AarService,
                pdlPortefoljeClient
        )
                , pdlIdentRepository,
                new BrukerServiceV2(pdlIdentRepository, oppfolgingsbrukerRepositoryV3, oppfolgingRepositoryV2),
                barnUnder18AarService,
                opensearchIndexer,
                opensearchIndexerV2,
                unleashService
        );
    }

    @AfterAll
    public static void cleanup() {
        db.update("truncate bruker_data CASCADE");
        db.update("truncate bruker_data_barn CASCADE");
        db.update("truncate foreldreansvar");
    }

    @Test
    @Timeout(25)
    public void testRepublishing() {
        int i = 0;
        while (i < 6000) {
            pdlBrukerdataKafkaService.behandleKafkaMeldingLogikk(randomPdlDokument);
            i++;
        }
    }
}