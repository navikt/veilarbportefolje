package no.nav.pto.veilarbportefolje.persononinfo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.pto.veilarbportefolje.client.AktorClient;
import no.nav.pto.veilarbportefolje.config.ApplicationConfigTest;
import no.nav.pto.veilarbportefolje.opensearch.OpensearchIndexer;
import no.nav.pto.veilarbportefolje.oppfolging.OppfolgingRepositoryV2;
import no.nav.pto.veilarbportefolje.oppfolgingsbruker.OppfolgingsbrukerRepositoryV3;
import no.nav.common.types.identer.AktorId;
import no.nav.common.types.identer.Fnr;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlDokument;
import no.nav.pto.veilarbportefolje.persononinfo.PdlResponses.PdlIdentResponse;
import no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarRepository;
import no.nav.pto.veilarbportefolje.persononinfo.barnUnder18Aar.BarnUnder18AarService;
import no.nav.pto.veilarbportefolje.service.BrukerServiceV2;
import no.nav.pto.veilarbportefolje.util.EndToEndTest;
import no.nav.pto.veilarbportefolje.util.SingletonPostgresContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static no.nav.pto.veilarbportefolje.util.TestUtil.readFileAsJsonString;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.AKTORID;
import static no.nav.pto.veilarbportefolje.persononinfo.domene.PDLIdent.Gruppe.FOLKEREGISTERIDENT;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomAktorId;
import static no.nav.pto.veilarbportefolje.util.TestDataUtils.randomFnr;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;

@SpringBootTest(classes = ApplicationConfigTest.class)
public class PdlBrukerdataKafkaServiceTest extends EndToEndTest {

    private final PdlDokument randomPdlDokument;

    private static PdlBrukerdataKafkaService pdlBrukerdataKafkaService;

    private static JdbcTemplate db;

    private static PdlIdentRepository pdlIdentRepository;

    private static OppfolgingRepositoryV2 oppfolgingRepositoryV2;

    private static IdentEndringService identEndringService;

    private static OpensearchIndexer opensearchIndexer;

    public PdlBrukerdataKafkaServiceTest() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String pdlDokumentAsString = readFileAsJsonString("/PDL_Files/pdl_dokument.json", getClass());
        randomPdlDokument = mapper.readValue(pdlDokumentAsString, PdlDokument.class);

        db = SingletonPostgresContainer.init().createJdbcTemplate();
        BarnUnder18AarRepository barnUnder18AarRepository = new BarnUnder18AarRepository(db, db);
        pdlIdentRepository = new PdlIdentRepository(db);
        PdlPersonRepository pdlPersonRepository = new PdlPersonRepository(db, db);
        OppfolgingsbrukerRepositoryV3 oppfolgingsbrukerRepositoryV3 = new OppfolgingsbrukerRepositoryV3(db, null);
        oppfolgingRepositoryV2 = new OppfolgingRepositoryV2(db);
        identEndringService = Mockito.mock(IdentEndringService.class);
        opensearchIndexer = Mockito.mock(OpensearchIndexer.class);

        db.update("truncate bruker_data CASCADE");
        db.update("truncate bruker_data_barn CASCADE");
        db.update("truncate foreldreansvar");

        PdlPortefoljeClient pdlPortefoljeClient = Mockito.mock(PdlPortefoljeClient.class);
        Mockito.when(pdlPortefoljeClient.hentBrukerBarnDataBolkFraPdl(any())).thenReturn(Map.of());

        BarnUnder18AarService barnUnder18AarService = new BarnUnder18AarService(barnUnder18AarRepository, pdlPortefoljeClient);

        pdlBrukerdataKafkaService = new PdlBrukerdataKafkaService(new PdlService(
                pdlIdentRepository,
                pdlPersonRepository,
                barnUnder18AarService,
                pdlPortefoljeClient
        )
                , pdlIdentRepository,
            new BrukerServiceV2(pdlIdentRepository, oppfolgingsbrukerRepositoryV3, oppfolgingRepositoryV2, Mockito.mock(AktorClient.class)),
                barnUnder18AarService,
                opensearchIndexer,
                identEndringService
        );
    }

    @BeforeEach
    public void resetMocks() {
        Mockito.reset(identEndringService, opensearchIndexer);
    }

    @AfterAll
    public static void cleanup() {
        db.update("truncate bruker_data CASCADE");
        db.update("truncate bruker_data_barn CASCADE");
        db.update("truncate foreldreansvar");
    }

    @Test
    @Timeout(90)
    public void testRepublishing() {
        int i = 0;
        while (i < 1000) {
            pdlBrukerdataKafkaService.behandleKafkaMeldingLogikk(randomPdlDokument);
            i++;
        }
    }

    @Test
    public void nyttFnr_skalTriggeIdentEndring() {
        AktorId aktorId = randomAktorId();
        Fnr gammeltFnr = randomFnr();
        Fnr nyttFnr = randomFnr();
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now(), UUID.randomUUID());
        pdlIdentRepository.upsertIdenter(List.of(
                new PDLIdent(aktorId.get(), false, AKTORID),
                new PDLIdent(gammeltFnr.get(), false, FOLKEREGISTERIDENT)
        ));

        pdlBrukerdataKafkaService.behandleKafkaMeldingLogikk(lagPdlDokument(List.of(
                new PDLIdent(aktorId.get(), false, AKTORID),
                new PDLIdent(gammeltFnr.get(), true, FOLKEREGISTERIDENT),
                new PDLIdent(nyttFnr.get(), false, FOLKEREGISTERIDENT)
        )));

        InOrder inOrder = Mockito.inOrder(identEndringService, opensearchIndexer);
        inOrder.verify(identEndringService).handterNyttFnr(aktorId, gammeltFnr, nyttFnr);
        inOrder.verify(opensearchIndexer).indekser(aktorId);
    }

    @Test
    public void uendretFnr_skalIkkeTriggeIdentEndring() {
        AktorId aktorId = randomAktorId();
        Fnr fnr = randomFnr();
        List<PDLIdent> identer = List.of(
                new PDLIdent(aktorId.get(), false, AKTORID),
                new PDLIdent(fnr.get(), false, FOLKEREGISTERIDENT)
        );
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now(), UUID.randomUUID());
        pdlIdentRepository.upsertIdenter(identer);

        pdlBrukerdataKafkaService.behandleKafkaMeldingLogikk(lagPdlDokument(identer));

        verifyNoInteractions(identEndringService);
    }

    @Test
    public void ingenLagredeIdenter_skalIkkeTriggeIdentEndring() {
        AktorId aktorId = randomAktorId();
        oppfolgingRepositoryV2.settUnderOppfolging(aktorId, ZonedDateTime.now(), UUID.randomUUID());

        pdlBrukerdataKafkaService.behandleKafkaMeldingLogikk(lagPdlDokument(List.of(
                new PDLIdent(aktorId.get(), false, AKTORID),
                new PDLIdent(randomFnr().get(), false, FOLKEREGISTERIDENT)
        )));

        verifyNoInteractions(identEndringService);
    }

    private PdlDokument lagPdlDokument(List<PDLIdent> identer) {
        return PdlDokument.builder()
                .hentPerson(randomPdlDokument.getHentPerson())
                .hentIdenter(new PdlIdentResponse.HentIdenterResponseData.HentIdenterResponsData().setIdenter(identer))
                .build();
    }
}
