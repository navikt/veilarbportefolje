package no.nav.pto.veilarbportefolje.persononinfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.getunleash.DefaultUnleash;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexerV2;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlDokument;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarRepository;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Map;

import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class PdlBrukerdataKafkaServiceTest extends EndToEndTest {

    private final PdlDokument randomPdlDokument;

    private static PdlBrukerdataKafkaService pdlBrukerdataKafkaService;

    @MockBean
    private static OpensearchIndexer opensearchIndexer;
    @MockBean
    private static OpensearchIndexerV2 opensearchIndexerV2;

    private static JdbcTemplate db;

    public PdlBrukerdataKafkaServiceTest() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String pdlDokumentAsString = readFileAsJsonString("/PDL_Files/pdl_dokument.json", getClass());
        randomPdlDokument = mapper.readValue(pdlDokumentAsString, PdlDokument.class);

        db = SingletonPostgresContainer.init().createJdbcTemplate();
        BarnUnder18AarRepository barnUnder18AarRepository = new BarnUnder18AarRepository(db, db);
        PdlIdentRepository pdlIdentRepository = new PdlIdentRepository(db);
        PdlPersonRepository pdlPersonRepository = new PdlPersonRepository(db, db);
        OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3 = new OppfolgingsbrukerRepositoryV3(db, null);
        OppfolgingRepositoryV2 oppfolgingRepositoryV2 = new OppfolgingRepositoryV2(db);

        db.update("truncate bruker_data CASCADE");
        db.update("truncate bruker_data_barn CASCADE");
        db.update("truncate foreldreansvar");

        PdlPortefoljeClient pdlPortefoljeClient = Mockito.mock(PdlPortefoljeClient.class);
        Mockito.when(pdlPortefoljeClient.hentBrukerBarnDataBolkFraPdl(any())).thenReturn(Map.of());

        BarnUnder18AarService barnUnder18AarService = new BarnUnder18AarService(barnUnder18AarRepository, pdlPortefoljeClient);

        DefaultUnleash mockUnleash = Mockito.mock(DefaultUnleash.class);
        Mockito.when(mockUnleash.isEnabled(any())).thenReturn(true);

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
                mockUnleash
        );
    }

    @AfterAll
    public static void cleanup() {
        db.update("truncate bruker_data CASCADE");
        db.update("truncate bruker_data_barn CASCADE");
        db.update("truncate foreldreansvar");
    }

    @Test
    @Timeout(20)
    public void testRepublishing() {
        int i = 0;
        while (i < 6000) {
            pdlBrukerdataKafkaService.behandleKafkaMeldingLogikk(randomPdlDokument);
            i++;
        }
    }
}
